# Dify RAG 召回评测

本目录用于独立评测 Dify 知识库检索召回效果。脚本只调用 Dify 知识库 `retrieve` 接口，不修改 Android 业务代码，也不修改现有 FastAPI 问答逻辑。

当前项目按业务域使用多个 Dify 知识库，例如医疗就医导航、养老机构信息和政务办事问答。评测时应坚持一个知识库对应一套问题集，不要把三个库混在一起测。当前内置的 `rag_recall_cases.json` 是港澳通行证与签注题集，应优先用于政务办事知识库。

建议按如下关系维护题集：

- `backend/evaluation/cases/policy_cases.json`：对应政务办事知识库。
- `backend/evaluation/cases/medical_cases.json`：对应医疗就医导航知识库。
- `backend/evaluation/cases/eldercare_cases.json`：对应养老机构知识库。

## 先列出知识库

如果还不确定 `DIFY_DATASET_ID`，可以先用辅助脚本列出当前 API Key 可访问的知识库：

PowerShell:

```powershell
$env:DIFY_API_BASE_URL="https://api.dify.ai/v1"
$env:DIFY_DATASET_API_KEY="your_dataset_api_key"
python backend/scripts/list_dify_datasets.py
```

Linux/macOS:

```bash
export DIFY_API_BASE_URL="https://api.dify.ai/v1"
export DIFY_DATASET_API_KEY="your_dataset_api_key"
python backend/scripts/list_dify_datasets.py
```

脚本会打印每个知识库的 `id`、`name`、`description`、`document_count`、`word_count`、`updated_at`。选择正确的 `id` 后，将它设置为 `DIFY_DATASET_ID` 再运行召回评测。

如果用港澳通行证题集测试时看到 Top1 文档大量来自医院、养老机构或其他非政务资料，优先检查 `DIFY_DATASET_ID` 是否选错。只有确认知识库名称与题集业务域匹配后，再考虑调整 Top K、分数阈值或知识库内容。

## 运行召回评测

PowerShell:

```powershell
$env:DIFY_API_BASE_URL="https://api.dify.ai/v1"
$env:DIFY_DATASET_ID="your_dataset_id"
$env:DIFY_DATASET_API_KEY="your_dataset_api_key"
$env:RAG_RECALL_TOP_K="5"
python backend/scripts/eval_dify_retrieval.py
```

Linux/macOS:

```bash
export DIFY_API_BASE_URL="https://api.dify.ai/v1"
export DIFY_DATASET_ID="your_dataset_id"
export DIFY_DATASET_API_KEY="your_dataset_api_key"
export RAG_RECALL_TOP_K="5"
python backend/scripts/eval_dify_retrieval.py
```

如果使用本地或私有部署 Dify，将 `DIFY_API_BASE_URL` 改为实际 API 地址，例如：

```powershell
$env:DIFY_API_BASE_URL="http://localhost/v1"
```

可选变量：

- `RAG_RECALL_SCORE_THRESHOLD`：检索分数阈值，不设置则由 Dify 默认行为决定。
- `RAG_RECALL_TIMEOUT_SECONDS`：HTTP 请求超时时间，默认 `60` 秒。
- `RAG_RECALL_CASES_PATH`：测试题集路径，不设置时默认读取 `backend/evaluation/rag_recall_cases.json`。
- `RAG_RECALL_REPORT_PREFIX`：报告文件名前缀，不设置时默认生成 `rag_recall_*`；设置为 `policy` 时生成 `policy_recall_*`，设置为 `medical` 时生成 `medical_recall_*`，设置为 `eldercare` 时生成 `eldercare_recall_*`。

## 三个知识库分别测试

政务办事知识库：

```powershell
$env:DIFY_DATASET_ID="your_policy_dataset_id"
$env:RAG_RECALL_CASES_PATH="backend/evaluation/cases/policy_cases.json"
$env:RAG_RECALL_REPORT_PREFIX="policy"
python backend/scripts/eval_dify_retrieval.py
```

医疗就医导航知识库：

```powershell
$env:DIFY_DATASET_ID="your_medical_dataset_id"
$env:RAG_RECALL_CASES_PATH="backend/evaluation/cases/medical_cases.json"
$env:RAG_RECALL_REPORT_PREFIX="medical"
python backend/scripts/eval_dify_retrieval.py
```

养老机构知识库：

```powershell
$env:DIFY_DATASET_ID="your_eldercare_dataset_id"
$env:RAG_RECALL_CASES_PATH="backend/evaluation/cases/eldercare_cases.json"
$env:RAG_RECALL_REPORT_PREFIX="eldercare"
python backend/scripts/eval_dify_retrieval.py
```

以上示例默认沿用已设置的 `DIFY_API_BASE_URL`、`DIFY_DATASET_API_KEY`、`RAG_RECALL_TOP_K`、`RAG_RECALL_SCORE_THRESHOLD` 和 `RAG_RECALL_TIMEOUT_SECONDS`。

## 输出文件

运行成功后会生成：

- `backend/evaluation/reports/rag_recall_report.md`
- `backend/evaluation/reports/rag_recall_results.csv`
- `backend/evaluation/reports/rag_recall_raw.json`
- `backend/evaluation/reports/rag_recall_results.xlsx`，仅在安装 `openpyxl` 或 `pandas` 时生成

当前项目已有 `httpx` 依赖，脚本默认使用它发起请求。若需要 Excel 输出，可额外安装：

```bash
pip install openpyxl
```

## 判定规则

- Top1 命中：第 1 个召回片段包含 `expected_keywords` 中任意关键词。
- Top3 命中：前 3 个召回片段任意一个包含 `expected_keywords` 中任意关键词。
- TopK 命中：前 K 个召回片段任意一个包含 `expected_keywords` 中任意关键词。
- 文档命中：召回文档名包含 `expected_doc_keywords` 中任意关键词。

这些规则只是关键词初判，不等同于语义完全正确。报告中的失败样例、低分样例和边界样例仍建议人工复核。

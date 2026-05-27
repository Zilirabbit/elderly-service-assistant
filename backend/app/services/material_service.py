from app.schemas.material_schema import (
    MaterialChecklist,
    MaterialItem,
    MaterialRequirement,
    SaveMaterialResponse,
)


_CHECKLISTS: dict[str, MaterialChecklist] = {
    "hk_macau_pass_apply": MaterialChecklist(
        code="hk_macau_pass_apply",
        title="办理港澳通行证",
        tips=[
            "首次办理一般需要本人到出入境窗口办理。",
            "办理前建议先确认当地出入境大厅预约要求。",
            "照片回执和申请表要求可能因城市略有差异。",
        ],
        requirements=[
            MaterialRequirement(
                id="resident_id",
                name="居民身份证原件",
                description="用于核验本人身份，建议同时准备一份复印件。",
                required=True,
                note="未满 16 周岁或特殊情况请按窗口要求补充监护材料。",
            ),
            MaterialRequirement(
                id="photo_receipt",
                name="出入境证件照片回执",
                description="到有资质照相点拍摄后取得照片回执。",
                required=True,
            ),
            MaterialRequirement(
                id="application_form",
                name="中国公民出入境证件申请表",
                description="可现场填写，也可按当地平台要求提前填写。",
                required=True,
            ),
            MaterialRequirement(
                id="appointment_record",
                name="预约记录或预约短信",
                description="如果当地要求预约，请带上预约成功记录。",
                required=False,
                note="演示清单仅作办理前核对，最终以窗口要求为准。",
            ),
        ],
    ),
    "hk_macau_endorsement_renew": MaterialChecklist(
        code="hk_macau_endorsement_renew",
        title="港澳通行证续签",
        tips=[
            "先确认港澳通行证仍在有效期内。",
            "部分地区可用智能签注设备办理，特殊情况需到窗口。",
            "如证件损坏、过期或信息变更，可能需要重新办理证件。",
        ],
        requirements=[
            MaterialRequirement(
                id="travel_pass",
                name="有效港澳通行证",
                description="请确认通行证未过期、未损坏，个人信息清晰可识别。",
                required=True,
            ),
            MaterialRequirement(
                id="resident_id",
                name="居民身份证原件",
                description="用于现场身份核验或智能设备核验。",
                required=True,
            ),
            MaterialRequirement(
                id="endorsement_info",
                name="原有签注信息",
                description="用于判断签注是否过期、是否仍有次数。",
                required=False,
            ),
            MaterialRequirement(
                id="payment",
                name="缴费方式",
                description="准备银行卡、手机支付或现场支持的缴费方式。",
                required=False,
            ),
        ],
    ),
    "border_crossing_prepare": MaterialChecklist(
        code="border_crossing_prepare",
        title="过关材料准备",
        tips=[
            "出发前先检查证件和签注有效性。",
            "常用药品建议保留原包装，并避免携带不明药品。",
            "老人出行建议提前保存家属电话和紧急联系人。",
        ],
        requirements=[
            MaterialRequirement(
                id="travel_pass",
                name="港澳通行证",
                description="过关时需要出示，建议放在容易拿取的位置。",
                required=True,
            ),
            MaterialRequirement(
                id="valid_endorsement",
                name="有效签注",
                description="确认目的地、次数和有效期是否满足本次出行。",
                required=True,
            ),
            MaterialRequirement(
                id="resident_id",
                name="居民身份证",
                description="部分交通、住宿或现场核验可能需要使用。",
                required=False,
            ),
            MaterialRequirement(
                id="medicine",
                name="常用药品",
                description="准备当天和备用药量，药品名称尽量清楚。",
                required=False,
            ),
            MaterialRequirement(
                id="emergency_contact",
                name="紧急联系人信息",
                description="写下家属姓名、电话，建议纸质和手机各保存一份。",
                required=False,
            ),
        ],
    ),
}

_SAVED_CHECKLISTS: dict[str, list[str]] = {}


class MaterialService:
    def list_items(self) -> list[MaterialItem]:
        return [
            MaterialItem(
                code=checklist.code,
                title=checklist.title,
                subtitle=self._build_subtitle(checklist),
                category="港澳通关",
            )
            for checklist in _CHECKLISTS.values()
        ]

    def get_checklist(self, item_code: str) -> MaterialChecklist | None:
        return _CHECKLISTS.get(item_code)

    def save_checklist(
        self,
        item_code: str,
        checked_requirement_ids: list[str],
    ) -> SaveMaterialResponse | None:
        checklist = self.get_checklist(item_code)
        if checklist is None:
            return None

        valid_ids = {item.id for item in checklist.requirements}
        checked_ids = [item_id for item_id in checked_requirement_ids if item_id in valid_ids]
        _SAVED_CHECKLISTS[item_code] = checked_ids

        return SaveMaterialResponse(
            item_code=item_code,
            checked_requirement_ids=checked_ids,
            saved_count=len(checked_ids),
            message="材料清单已保存到本机演示记录",
        )

    @staticmethod
    def _build_subtitle(checklist: MaterialChecklist) -> str:
        names = [item.name for item in checklist.requirements[:3]]
        return "、".join(names)


material_service = MaterialService()

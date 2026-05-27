from pydantic import BaseModel, Field


class MaterialItem(BaseModel):
    code: str
    title: str
    subtitle: str
    category: str


class MaterialRequirement(BaseModel):
    id: str
    name: str
    description: str
    required: bool = True
    note: str | None = None


class MaterialChecklist(BaseModel):
    code: str
    title: str
    tips: list[str] = Field(default_factory=list)
    requirements: list[MaterialRequirement] = Field(default_factory=list)


class SaveMaterialRequest(BaseModel):
    item_code: str
    checked_requirement_ids: list[str] = Field(default_factory=list)


class SaveMaterialResponse(BaseModel):
    item_code: str
    checked_requirement_ids: list[str] = Field(default_factory=list)
    saved_count: int
    message: str

from fastapi import APIRouter, HTTPException

from app.schemas.material_schema import (
    MaterialChecklist,
    MaterialItem,
    SaveMaterialRequest,
    SaveMaterialResponse,
)
from app.services.material_service import material_service

router = APIRouter(prefix="/api/v1", tags=["materials"])


@router.get("/materials/items", response_model=list[MaterialItem])
async def list_material_items() -> list[MaterialItem]:
    return material_service.list_items()


@router.get("/materials/{item_code}", response_model=MaterialChecklist)
async def get_material_checklist(item_code: str) -> MaterialChecklist:
    checklist = material_service.get_checklist(item_code)
    if checklist is None:
        raise HTTPException(status_code=404, detail="未找到该材料清单")
    return checklist


@router.post("/materials/save", response_model=SaveMaterialResponse)
async def save_material_checklist(req: SaveMaterialRequest) -> SaveMaterialResponse:
    result = material_service.save_checklist(
        item_code=req.item_code,
        checked_requirement_ids=req.checked_requirement_ids,
    )
    if result is None:
        raise HTTPException(status_code=404, detail="未找到该材料清单")
    return result

package com.lttclaw.metronome.viewmodel

import com.lttclaw.metronome.model.Section
import com.lttclaw.metronome.util.PlanStorage
import me.hgj.jetpackmvvm.base.viewmodel.BaseViewModel

class SectionListViewModel : BaseViewModel() {

    private var currentPlanId: String = ""
    private var currentPlanName: String = ""

    fun loadListData(planId: String?): List<Section> {
        val plan = PlanStorage.getPlanOrSelected(planId)
        currentPlanId = plan.id
        currentPlanName = plan.name
        return PlanStorage.copySections(plan.sections)
    }

    fun saveListData(listData: List<Any?>?) {
        val sections = listData?.filterIsInstance<Section>().orEmpty()
        val effectivePlanId = if (currentPlanId.isBlank()) {
            PlanStorage.getPlanOrSelected(null).id
        } else {
            currentPlanId
        }
        PlanStorage.updatePlanSections(effectivePlanId, sections)
    }

    fun getCurrentPlanId(): String = currentPlanId.ifBlank { PlanStorage.getPlanOrSelected(null).id }

    fun getCurrentPlanName(): String = currentPlanName.ifBlank { PlanStorage.getPlanOrSelected(null).name }
}

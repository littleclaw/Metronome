package com.lttclaw.metronome.util

import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.lttclaw.metronome.model.Section
import com.lttclaw.metronome.model.SectionPlan

const val SP_KEY = "listData"
const val SP_PLANS_KEY = "sectionPlans"
const val SP_SELECTED_PLAN_ID_KEY = "selectedPlanId"
const val DEFAULT_PLAN_NAME = "方案1"

object PlanStorage {

    fun loadPlans(): MutableList<SectionPlan> {
        val sp = SPUtils.getInstance()
        val savedPlans = runCatching {
            val raw = sp.getString(SP_PLANS_KEY, "")
            if (raw.isBlank()) {
                mutableListOf()
            } else {
                GsonUtils.fromJson<List<SectionPlan>>(
                    raw,
                    GsonUtils.getListType(SectionPlan::class.java)
                )?.map(::normalizePlan)?.toMutableList() ?: mutableListOf()
            }
        }.getOrDefault(mutableListOf())

        val plans = if (savedPlans.isNotEmpty()) {
            savedPlans
        } else {
            createInitialPlans(sp)
        }

        val selectedPlanId = sp.getString(SP_SELECTED_PLAN_ID_KEY, "")
        val selectedExists = plans.any { it.id == selectedPlanId }
        val effectiveSelectedPlanId = if (selectedExists) {
            selectedPlanId
        } else {
            plans.first().id
        }

        savePlans(plans, effectiveSelectedPlanId)
        return plans
    }

    fun savePlans(plans: List<SectionPlan>, selectedPlanId: String? = null) {
        val sp = SPUtils.getInstance()
        val normalizedPlans = plans.map(::normalizePlan)
        sp.put(SP_PLANS_KEY, GsonUtils.toJson(normalizedPlans))
        val targetSelectedPlanId = selectedPlanId
            ?: sp.getString(SP_SELECTED_PLAN_ID_KEY, "")
                .takeIf { currentId -> normalizedPlans.any { it.id == currentId } }
            ?: normalizedPlans.firstOrNull()?.id
            ?: ""
        sp.put(SP_SELECTED_PLAN_ID_KEY, targetSelectedPlanId)
    }

    fun setSelectedPlanId(planId: String) {
        val plans = loadPlans()
        val effectivePlanId = plans.firstOrNull { it.id == planId }?.id ?: plans.first().id
        SPUtils.getInstance().put(SP_SELECTED_PLAN_ID_KEY, effectivePlanId)
    }

    fun getSelectedPlanId(): String {
        val plans = loadPlans()
        val selectedPlanId = SPUtils.getInstance().getString(SP_SELECTED_PLAN_ID_KEY, "")
        return plans.firstOrNull { it.id == selectedPlanId }?.id ?: plans.first().id
    }

    fun getPlanOrSelected(planId: String?): SectionPlan {
        val plans = loadPlans()
        return plans.firstOrNull { it.id == planId }
            ?: plans.firstOrNull { it.id == getSelectedPlanId() }
            ?: plans.first()
    }

    fun updatePlanSections(planId: String, sections: List<Section>) {
        val plans = loadPlans()
        val targetPlan = plans.firstOrNull { it.id == planId }
            ?: plans.firstOrNull { it.id == getSelectedPlanId() }
            ?: plans.first()
        val updatedPlans = plans.map { plan ->
            if (plan.id == targetPlan.id) {
                plan.copy(sections = copySections(sections))
            } else {
                normalizePlan(plan)
            }
        }
        savePlans(updatedPlans, targetPlan.id)
    }

    fun copySections(sections: List<Section>): MutableList<Section> {
        return sections.map { section ->
            Section(section.repeatNum, section.length, section.delay, false)
        }.toMutableList()
    }

    private fun createInitialPlans(sp: SPUtils): MutableList<SectionPlan> {
        val migratedSections = runCatching {
            val raw = sp.getString(SP_KEY, "")
            if (raw.isBlank()) {
                emptyList()
            } else {
                GsonUtils.fromJson<List<Section>>(
                    raw,
                    GsonUtils.getListType(Section::class.java)
                ) ?: emptyList()
            }
        }.getOrDefault(emptyList())

        val sections = if (migratedSections.isNotEmpty()) {
            copySections(migratedSections)
        } else {
            defaultSections()
        }

        val defaultPlan = SectionPlan(
            id = DEFAULT_PLAN_NAME,
            name = DEFAULT_PLAN_NAME,
            sections = sections
        )
        sp.remove(SP_KEY)
        return mutableListOf(defaultPlan)
    }

    private fun normalizePlan(plan: SectionPlan): SectionPlan {
        val normalizedName = plan.name.trim().ifEmpty { DEFAULT_PLAN_NAME }
        val normalizedId = plan.id.ifBlank { normalizedName }
        return SectionPlan(
            id = normalizedId,
            name = normalizedName,
            sections = copySections(plan.sections)
        )
    }

    private fun defaultSections(): MutableList<Section> {
        return mutableListOf(
            Section(11, 4000L, 7000L),
            Section(12, 5000L, 8000L),
            Section(12, 3000L, 1000L)
        )
    }
}

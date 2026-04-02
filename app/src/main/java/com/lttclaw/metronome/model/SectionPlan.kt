package com.lttclaw.metronome.model

data class SectionPlan(
    val id: String,
    val name: String,
    val sections: MutableList<Section>
)

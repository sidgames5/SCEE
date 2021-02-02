package de.westnordost.streetcomplete.data.quest

import de.westnordost.osmapi.map.MapDataWithGeometry
import de.westnordost.osmapi.map.data.Element
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.changes.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.osmquest.OsmElementQuestType
import de.westnordost.streetcomplete.quests.AbstractQuestAnswerFragment

open class TestQuestTypeA : OsmElementQuestType<String> {

    override fun getTitle(tags: Map<String, String>) = 0
    override fun isApplicableTo(element: Element):Boolean? = null
    override fun applyAnswerTo(answer: String, changes: StringMapChangesBuilder) {}
    override val icon = 0
    override fun createForm(): AbstractQuestAnswerFragment<String> = object : AbstractQuestAnswerFragment<String>() {}
    override val commitMessage = ""
    override fun getApplicableElements(mapData: MapDataWithGeometry) = emptyList<Element>()
    override val wikiLink: String? = null
}

class TestQuestTypeB : TestQuestTypeA()
class TestQuestTypeC : TestQuestTypeA()
class TestQuestTypeD : TestQuestTypeA()

class TestQuestTypeDisabled : TestQuestTypeA() {
    override val defaultDisabledMessage = R.string.default_disabled_msg_go_inside
}

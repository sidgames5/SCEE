package de.westnordost.streetcomplete.data.quest

import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.map.data.Element
import de.westnordost.osmapi.map.data.OsmLatLon
import de.westnordost.streetcomplete.any
import de.westnordost.streetcomplete.data.osm.elementgeometry.ElementPointGeometry
import de.westnordost.streetcomplete.data.osm.osmquest.OsmElementQuestType
import de.westnordost.streetcomplete.data.osm.osmquest.OsmQuest
import de.westnordost.streetcomplete.data.osm.osmquest.OsmQuestSource
import de.westnordost.streetcomplete.data.osmnotes.notequests.OsmNoteQuest
import de.westnordost.streetcomplete.data.osmnotes.notequests.OsmNoteQuestSource
import de.westnordost.streetcomplete.data.osmnotes.notequests.OsmNoteQuestType
import de.westnordost.streetcomplete.data.visiblequests.VisibleQuestTypeSource
import de.westnordost.streetcomplete.eq
import de.westnordost.streetcomplete.mock
import de.westnordost.streetcomplete.on
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.invocation.InvocationOnMock

class VisibleQuestsSourceTest {

    private lateinit var osmQuestSource: OsmQuestSource
    private lateinit var osmNoteQuestSource: OsmNoteQuestSource
    private lateinit var visibleQuestTypeSource: VisibleQuestTypeSource
    private lateinit var source: VisibleQuestsSource

    private lateinit var noteQuestListener: OsmNoteQuestSource.Listener
    private lateinit var questListener: OsmQuestSource.Listener
    private lateinit var visibleQuestTypeListener: VisibleQuestTypeSource.Listener

    private lateinit var listener: VisibleQuestsSource.Listener

    private val bbox = BoundingBox(0.0,0.0,1.0,1.0)
    private val questTypes = listOf("a","b","c")

    @Before fun setUp() {
        osmNoteQuestSource = mock()
        osmQuestSource = mock()
        visibleQuestTypeSource = mock()

        on(visibleQuestTypeSource.isVisible(any())).thenReturn(true)

        on(osmNoteQuestSource.addListener(any())).then { invocation: InvocationOnMock ->
            noteQuestListener = (invocation.arguments[0] as OsmNoteQuestSource.Listener)
            Unit
        }
        on(osmQuestSource.addListener(any())).then { invocation: InvocationOnMock ->
            questListener = (invocation.arguments[0] as OsmQuestSource.Listener)
            Unit
        }
        on(visibleQuestTypeSource.addListener(any())).then { invocation: InvocationOnMock ->
            visibleQuestTypeListener = (invocation.arguments[0] as VisibleQuestTypeSource.Listener)
            Unit
        }

        source = VisibleQuestsSource(osmQuestSource, osmNoteQuestSource, visibleQuestTypeSource)

        listener = mock()
        source.addListener(listener)
    }

    @Test fun getAllCount() {
        on(osmQuestSource.getAllInBBoxCount(bbox)).thenReturn(3)

        assertEquals(3, source.getCount(bbox))
    }

    @Test fun getAllVisible() {
        on(osmQuestSource.getAllVisibleInBBox(bbox, questTypes)).thenReturn(listOf(mock(), mock(), mock()))
        on(osmNoteQuestSource.getAllVisibleInBBox(bbox)).thenReturn(listOf(mock(), mock()))

        val quests = source.getAllVisible(bbox, questTypes)
        assertEquals(5, quests.size)
        val osmQuests = quests.filter { it.group == QuestGroup.OSM && it.quest is OsmQuest }
        assertEquals(3, osmQuests.size)
        val osmNoteQuests = quests.filter { it.group == QuestGroup.OSM_NOTE && it.quest is OsmNoteQuest }
        assertEquals(2, osmNoteQuests.size)
    }

    @Test fun `getAllVisible returns only quests of types that are visible`() {
        val t1 = TestQuestTypeA()
        val t2 = TestQuestTypeB()
        val q1 = osmQuest(1L, t1)
        val q2 = osmQuest(2L, t2)
        val questTypes = listOf(t2.javaClass.simpleName)
        on(osmQuestSource.getAllVisibleInBBox(bbox, questTypes)).thenReturn(listOf(q1, q2))
        on(visibleQuestTypeSource.isVisible(t1)).thenReturn(false)
        on(visibleQuestTypeSource.isVisible(t2)).thenReturn(true)


        val quests = source.getAllVisible(bbox, questTypes)
        assertEquals(q2, quests.single())
    }

    @Test fun `osm quests added or removed triggers listener`() {
        val quests = listOf(osmQuest(1L), osmQuest(2L))
        val deleted = listOf(3L,4L)
        questListener.onUpdated(quests, deleted)
        verify(listener).onUpdatedVisibleQuests(eq(quests), eq(deleted), QuestGroup.OSM)
    }

    @Test fun `osm quests added of invisible type does not trigger listener`() {
        val quests = listOf(osmQuest(1L), osmQuest(2L))
        on(visibleQuestTypeSource.isVisible(any())).thenReturn(false)
        questListener.onUpdated(quests, emptyList())
        verifyZeroInteractions(listener)
    }

    @Test fun `osm note quests added or removed triggers listener`() {
        val quests = listOf(osmNoteQuest(1L), osmNoteQuest(2L))
        val deleted = listOf(3L,4L)
        noteQuestListener.onUpdated(quests, deleted)
        verify(listener).onUpdatedVisibleQuests(eq(quests), eq(deleted), QuestGroup.OSM_NOTE)
    }

    @Test fun `osm note quests added of invisible type does not trigger listener`() {
        val quests = listOf(osmNoteQuest(1L), osmNoteQuest(2L))
        on(visibleQuestTypeSource.isVisible(any())).thenReturn(false)
        noteQuestListener.onUpdated(quests, emptyList())
        verifyZeroInteractions(listener)
    }

    @Test fun `trigger invalidate listener if quest type visibilities changed`() {
        visibleQuestTypeListener.onQuestTypeVisibilitiesChanged()
        verify(listener).onInvalidated()
    }

    @Test fun `trigger invalidate listener if visible note quests were invalidated`() {
        noteQuestListener.onInvalidated()
        verify(listener).onInvalidated()
    }

    private fun osmQuest(id: Long, questType: OsmElementQuestType<*> = mock()) =
        OsmQuest(id, questType, Element.Type.NODE, 1L, ElementPointGeometry(OsmLatLon(0.0,0.0)))

    private fun osmNoteQuest(id: Long) = OsmNoteQuest(id, mock(), OsmNoteQuestType())
}

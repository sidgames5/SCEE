package de.westnordost.streetcomplete.overlays.buildings

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.filter
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.BUILDING
import de.westnordost.streetcomplete.osm.building.BuildingType
import de.westnordost.streetcomplete.osm.building.BuildingType.*
import de.westnordost.streetcomplete.osm.building.createBuildingType
import de.westnordost.streetcomplete.osm.building.iconResName
import de.westnordost.streetcomplete.overlays.Color
import de.westnordost.streetcomplete.overlays.Overlay
import de.westnordost.streetcomplete.overlays.PolygonStyle
import de.westnordost.streetcomplete.quests.building_type.AddBuildingType

class BuildingsOverlay : Overlay {

    override val title = R.string.overlay_buildings
    override val icon = R.drawable.ic_quest_building
    override val changesetComment = "Survey buildings"
    override val wikiLink = "Key:building"
    override val achievements = listOf(BUILDING)
    override val hidesQuestTypes = setOf(AddBuildingType::class.simpleName!!)

    // building:use not supported, so don't offer to change it -> exclude from the overlay
    override fun getStyledElements(mapData: MapDataWithGeometry) = mapData.filter(
        """
            ways, relations with
              (
                building and building !~ no|entrance
                or historic ~ monument|ship|wreck
                or man_made ~ ${listOf(
                  "antenna",
                  "chimney",
                  "cooling_tower",
                  "communications_tower",
                  "gasometer",
                  "lighthouse",
                  "obelisk",
                  "silo",
                  "storage_tank",
                  "stupa",
                  "telescope",
                  "tower",
                  "watermill",
                  "water_tower",
                  "windmill",
                ).joinToString("|")}
              )
              and !building:use
        """)
        .map { element ->
            val building = createBuildingType(element.tags)

            val color = building?.color
                ?: if (isBuildingTypeMissing(element.tags)) Color.DATA_REQUESTED else Color.INVISIBLE

            element to PolygonStyle(color = color, icon = building?.iconResName)
        }

    override fun createForm(element: Element?) = BuildingsOverlayForm()

    private val BuildingType.color get() = when (this) {
        // ~detached homes
        DETACHED, SEMI_DETACHED, HOUSEBOAT, BUNGALOW, STATIC_CARAVAN, HUT, FARM, -> // 10%
            Color.BLUE

        // ~non-detached homes
        HOUSE, DORMITORY, APARTMENTS, TERRACE, -> // 52%
            Color.SKY

        // unspecified residential
        RESIDENTIAL, -> // 12%
            Color.CYAN

        // parking, sheds, outbuildings in general...
        OUTBUILDING, CARPORT, GARAGE, GARAGES, SHED, BOATHOUSE, SERVICE, TRANSFORMER_TOWER, ALLOTMENT_HOUSE,
        TENT, CONTAINER, GUARDHOUSE, -> // 11%
            Color.LIME

        // commercial, industrial, farm buildings
        COMMERCIAL, KIOSK, RETAIL, OFFICE, BRIDGE, HOTEL, PARKING,
        INDUSTRIAL, WAREHOUSE, HANGAR, STORAGE_TANK, DIGESTER,
        FARM_AUXILIARY, BARN, COWSHED, STABLE, STY, SILO, GREENHOUSE,
        ROOF -> // 5%
            Color.GOLD

        // amenity buildings
        TRAIN_STATION, TRANSPORTATION, TRANSIT_SHELTER, ELEVATOR,
        CIVIC, GOVERNMENT, FIRE_STATION, HOSPITAL,
        KINDERGARTEN, SCHOOL, COLLEGE, UNIVERSITY, SPORTS_CENTRE, STADIUM, GRANDSTAND, SPORTS_HALL, RIDING_HALL,
        RELIGIOUS, CHURCH, CHAPEL, CATHEDRAL, PRESBYTERY, MOSQUE, TEMPLE, PAGODA, SYNAGOGUE, SHRINE,
        TOILETS, -> // 2%
            Color.ORANGE

        // other/special
        HISTORIC, ABANDONED, RUINS, CONSTRUCTION, BUNKER, TOMB, TOWER,
        UNSUPPORTED ->
            Color.GRAY
    }

    private fun isBuildingTypeMissing(tags: Map<String, String>): Boolean =
        !BuildingType.otherKeysPotentiallyDescribingBuildingType.any { it in tags }
}

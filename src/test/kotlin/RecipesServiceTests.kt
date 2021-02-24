import com.andreromano.foodmix.db.IngredientService
import com.andreromano.foodmix.db.RecipeService
import com.andreromano.foodmix.models.*
import com.andreromano.foodmix.module
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class RecipesServiceTests {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    lateinit var recipeService: RecipeService

    companion object {
        @BeforeClass
        @JvmStatic
        fun startDb() {
            Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        }
    }

    @Before
    fun resetDb() {
        recipeService = RecipeService()
        transaction {
            SchemaUtils.create(
                Categories,
                Images,
                Ingredients,
                Users,
                Recipes,
                Ratings,
                RecipeCategories,
                RecipeIngredients,
            )

            Users.initializeTable()
            Categories.initializeTable()
            Ingredients.initializeTable()
//            Recipes.initializeTable()

        }
        transaction {
            recipes.forEach {
                runBlocking { recipeService.insert(it) }
            }
            Ratings.initializeTable()
        }
    }

    val recipes = listOf(
        InsertRecipe(
            userId = 1,
            title = "title 1",
            description = "description 1",
            image = byteArrayOf(0x01, 0x01),
            categories = listOf(1,2,3),
            cookingTime = 10,
            calories = 20,
            servings = 30,
            ingredients = listOf(1,2,3),
            directions = listOf(
                InsertDirection(
                    title = "dir title 11",
                    description = "dir description 11",
                    image = byteArrayOf(0x01, 0x02)
                ),
                InsertDirection(
                    title = "dir title 12",
                    description = "dir description 12",
                    image = byteArrayOf(0x01, 0x03)
                ),
            ),
        ),
        InsertRecipe(
            userId = 2,
            title = "title 2",
            description = "description 2",
            image = byteArrayOf(0x02, 0x01),
            categories = listOf(3),
            cookingTime = 11,
            calories = 21,
            servings = 31,
            ingredients = listOf(1,3,4,5),
            directions = listOf(
                InsertDirection(
                    title = "dir title 21",
                    description = "dir description 21",
                    image = byteArrayOf(0x02, 0x02)
                ),
            ),
        ),
    )

//    private fun areEqual(a: InsertRecipe, b: Recipe) =
//        a.userId == b.author.id &&
//        a.title == b.title &&
//        a.description == b.description &&
//        a.categories == b.categories &&
//        a.cookingTime == b.cookingTime &&
//        a.calories == b.calories &&
//        a.servings == b.servings &&
//        a.ingredients == b.ingredients &&
//        a.directions.size == b.directions.size &&
//        a.directions.filterIndexed { index, a -> areEquala(a, b[index]) }.isEmpty()
//
//    private fun areEquala(a: InsertDirection, b: Direction) =
//        a.title == b.title &&
//        a.description == b.description

    @Test
    fun test() = runBlocking {
        val all = recipeService.getAll()
        assert(all == recipes)

        val allContainingCategories1 = recipeService.getAllContainingCategory(1)
        assert(allContainingCategories1 == recipes.filter { it.categories.contains(1) })

        val allContainingCategories2 = recipeService.getAllContainingCategory(3)
        assert(allContainingCategories2 == recipes.filter { it.categories.contains(3) })

        val allContainingCategories3 = recipeService.getAllContainingCategory(10)
        assert(allContainingCategories3 == recipes.filter { it.categories.contains(10) })

        val allContainingAllIngredients1 = recipeService.getAllContainingAllIngredients(listOf(1,2), RecipeOrderBy.RELEVANCE)
        assert(allContainingAllIngredients1.containsAll(recipes.filter { it.ingredients.containsAll(listOf(1,2)) }))

        val allContainingAllIngredients2 = recipeService.getAllContainingAllIngredients(listOf(1,3), RecipeOrderBy.RELEVANCE)
        assert(allContainingAllIngredients2.containsAll(recipes.filter { it.ingredients.containsAll(listOf(1,3)) }))

        val allContainingAllIngredients3 = recipeService.getAllContainingAllIngredients(listOf(2,5), RecipeOrderBy.RELEVANCE)
        assert(allContainingAllIngredients3.containsAll(recipes.filter { it.ingredients.containsAll(listOf(2,5)) }))
    }


    private fun ResultRow.toIngredient(): Ingredient = Ingredient(
        id = this[Ingredients.id].value,
        name = this[Ingredients.name],
        imageUrl = this[Ingredients.imageId]?.value?.toImageUrl(),
        type = this[Ingredients.type]
    )

}
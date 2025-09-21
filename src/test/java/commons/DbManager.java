package commons;

import java.sql.*;

public class DbManager {
    private Connection conn;

    public DbManager(ConfigReader cfg) throws SQLException {
        String url = "jdbc:postgresql://" + cfg.dbHost + ":" + cfg.dbPort + "/" + cfg.dbName;
        conn = DriverManager.getConnection(url, cfg.dbUser, cfg.dbPassword);
    }

    // Create tables with full schema
    public void createTableIfNotExists(String tableName) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "recipe_id SERIAL PRIMARY KEY," +
                "recipe_name TEXT," +
                "recipe_category TEXT," +
                "food_category TEXT," +
                "ingredients TEXT," +
                "preparation_time TEXT," +
                "cooking_time TEXT," +
                "tag TEXT," +
                "no_of_servings TEXT," +
                "cuisine_category TEXT," +
                "recipe_description TEXT," +
                "preparation_method TEXT," +
                "nutrient_values TEXT," +
                "recipe_url TEXT UNIQUE" +
                ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void insertRecipe(String tableName, Recipe recipe) throws SQLException {
        String sql = "INSERT INTO " + tableName +
                " (recipe_name, recipe_category, food_category, ingredients, preparation_time, cooking_time, tag, no_of_servings, cuisine_category, recipe_description, preparation_method, nutrient_values, recipe_url)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (recipe_url) DO NOTHING";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipe.Recipe_Name);
            ps.setString(2, recipe.Recipe_Category);
            ps.setString(3, recipe.Food_Category);
            ps.setString(4, String.join(", ", recipe.Ingredients));
            ps.setString(5, recipe.Preparation_Time);
            ps.setString(6, recipe.Cooking_Time);
            ps.setString(7, recipe.Tag);
            ps.setString(8, recipe.No_of_servings);
            ps.setString(9, recipe.Cuisine_category);
            ps.setString(10, recipe.Recipe_Description);
            ps.setString(11, recipe.Preparation_method);
            ps.setString(12, recipe.Nutrient_values);
            ps.setString(13, recipe.Recipe_URL);
            ps.executeUpdate();
        }
    }

    public boolean recipeExists(String tableName, String recipeUrl) throws SQLException {
        String sql = "SELECT 1 FROM " + tableName + " WHERE recipe_url = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipeUrl);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public void close() throws SQLException {
        if (conn != null) conn.close();
    }

    public int getRowCount(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}

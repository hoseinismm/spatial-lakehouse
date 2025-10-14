import java.sql.*;


public class DataBase {
    public static void updateDatabase(String sql, Object... parameters) {
        try {
            Connection connection = MySQLConnection.getInstance().getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] instanceof String) {
                    preparedStatement.setString(i + 1, (String) parameters[i]);
                } else if (parameters[i] instanceof Integer) {
                    preparedStatement.setInt(i + 1, (Integer) parameters[i]);
                } else if (parameters[i] instanceof java.sql.Timestamp) {
                    preparedStatement.setTimestamp(i + 1, (java.sql.Timestamp) parameters[i]);
                } else if (parameters[i] instanceof java.sql.Date) {
                    preparedStatement.setDate(i + 1, (java.sql.Date) parameters[i]);
                } else {
                    throw new IllegalArgumentException("نوع پارامتر پشتیبانی نمی‌شود: " + parameters[i].getClass().getName());
                }
            }

            preparedStatement.executeUpdate();

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static ResultSet queryDatabase(String sql, Object... parameters) {
        ResultSet resultSet = null;
        try {
            Connection connection = MySQLConnection.getInstance().getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] instanceof String) {
                    preparedStatement.setString(i + 1, (String) parameters[i]);
                } else if (parameters[i] instanceof Integer) {
                    preparedStatement.setInt(i + 1, (Integer) parameters[i]);
                } else if (parameters[i] instanceof java.sql.Timestamp) {
                    preparedStatement.setTimestamp(i + 1, (java.sql.Timestamp) parameters[i]);
                } else if (parameters[i] instanceof java.sql.Date) {
                    preparedStatement.setDate(i + 1, (java.sql.Date) parameters[i]);
                } else {

                    throw new IllegalArgumentException("نوع پارامتر پشتیبانی نمی‌شود: " + parameters[i].getClass().getName());
                }
            }

            resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }


}





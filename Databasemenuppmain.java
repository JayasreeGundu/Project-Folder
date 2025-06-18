import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.sql.*;
import java.util.*;

public class DatabaseMenuApp1 extends Application {

    final String DB_URL = "jdbc:mysql://localhost:3306/companydb";
    final String DB_USER = "root";
    final String DB_PASS = "madhuri@123";

    VBox contentBox = new VBox(10);
    TableView<RowData> tableView = new TableView<>();
    TextField tableNameField = new TextField();
    TextField idColumnField = new TextField();
    List<String> columnNames = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Database Operations");

        HBox menuBar = new HBox(10);
        menuBar.setPadding(new Insets(10));
        String[] menuItems = {"Create", "Insert", "Update", "Delete", "Select"};
        for (String item : menuItems) {
            Button button = new Button(item);
            button.setFont(Font.font(14));
            button.setStyle("-fx-background-color: #7a5df1; -fx-text-fill: white;");
            button.setEffect(new DropShadow());
            button.setOnAction(e -> handleMenuClick(item));
            menuBar.getChildren().add(button);
        }

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        VBox root = new VBox(menuBar, scrollPane);
        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.show();
    }

    void handleMenuClick(String item) {
        contentBox.getChildren().clear();
        Label titleLabel = new Label(item + " From Table");
        titleLabel.setFont(Font.font(18));
        contentBox.getChildren().add(titleLabel);

        tableNameField.setPromptText("Enter Table Name");
        idColumnField.setPromptText("Enter ID Column (like EMPNO)");

        contentBox.getChildren().addAll(tableNameField, idColumnField);

        Button actionBtn = new Button();

        switch (item) {
            case "Delete":
                actionBtn.setText("Delete Selected Records");
                actionBtn.setOnAction(e -> deleteSelectedRecords());
                break;
            case "Select":
                actionBtn.setText("Fetch Records");
                actionBtn.setOnAction(e -> fetchRecords());
                break;
            case "Create":
                actionBtn.setText("Create Table (EMPNO, ENAME)");
                actionBtn.setOnAction(e -> createTable());
                break;
            case "Insert":
                actionBtn.setText("Insert Sample Record");
                actionBtn.setOnAction(e -> insertRecord());
                break;
            case "Update":
                actionBtn.setText("Update Selected Record's ENAME");
                actionBtn.setOnAction(e -> updateRecord());
                break;
        }

        actionBtn.setStyle("-fx-background-color: #6be0b4; -fx-font-weight: bold;");
        contentBox.getChildren().add(actionBtn);
        contentBox.getChildren().add(tableView);
    }

    void createTable() {
        String tableName = tableNameField.getText().trim();
        if (tableName.isEmpty()) {
            showAlert("Enter table name to create.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName +
                    " (EMPNO INT PRIMARY KEY, ENAME VARCHAR(100))";
            stmt.executeUpdate(sql);
            showAlert("Table created successfully.");
        } catch (SQLException e) {
            showAlert("SQL Error: " + e.getMessage());
        }
    }

    void insertRecord() {
        String tableName = tableNameField.getText().trim();
        if (tableName.isEmpty()) {
            showAlert("Enter table name to insert.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + tableName + " (EMPNO, ENAME) VALUES (?, ?)")) {

            int empno = (int) (Math.random() * 10000);
            pstmt.setInt(1, empno);
            pstmt.setString(2, "NEW_EMP");
            pstmt.executeUpdate();
            showAlert("Record inserted with EMPNO = " + empno);
            fetchRecords();

        } catch (SQLException e) {
            showAlert("SQL Error: " + e.getMessage());
        }
    }

    void updateRecord() {
        String tableName = tableNameField.getText().trim();
        String idColumn = idColumnField.getText().trim();
        if (tableName.isEmpty() || idColumn.isEmpty()) {
            showAlert("Please fill in Table Name and ID Column.");
            return;
        }

        int idColIndex = columnNames.indexOf(idColumn);
        int nameColIndex = columnNames.indexOf("ENAME");
        if (idColIndex == -1 || nameColIndex == -1) {
            showAlert("ID or ENAME column not found.");
            return;
        }

        ObservableList<RowData> rows = tableView.getItems();
        for (RowData row : rows) {
            if (row.isSelected()) {
                String idValue = row.getValues().get(idColIndex);

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                     Statement stmt = conn.createStatement()) {

                    String newName = "UPDATED_EMP";
                    String query = "UPDATE " + tableName + " SET ENAME='" + newName + "' WHERE " + idColumn + "='" + idValue + "'";
                    stmt.executeUpdate(query);
                    showAlert("Record with " + idColumn + "=" + idValue + " updated.");
                    fetchRecords();

                } catch (SQLException e) {
                    showAlert("SQL Error: " + e.getMessage());
                }

                break;
            }
        }
    }

    void fetchRecords() {
        String tableName = tableNameField.getText().trim();
        if (tableName.isEmpty()) return;

        tableView.getItems().clear();
        tableView.getColumns().clear();
        columnNames.clear();
        tableView.setEditable(true);

        ObservableList<RowData> rows = FXCollections.observableArrayList();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();

            TableColumn<RowData, Boolean> selectCol = new TableColumn<>("Select");
            selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
            selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
            selectCol.setEditable(true);
            tableView.getColumns().add(selectCol);

            for (int i = 1; i <= cols; i++) {
                final int colIndex = i - 1;
                String colName = rsmd.getColumnName(i);
                columnNames.add(colName);

                TableColumn<RowData, String> col = new TableColumn<>(colName);
                col.setCellValueFactory(cellData ->
                        new SimpleStringProperty(cellData.getValue().getValues().get(colIndex)));
                tableView.getColumns().add(col);
            }

            while (rs.next()) {
                List<String> rowValues = new ArrayList<>();
                for (int i = 1; i <= cols; i++) {
                    rowValues.add(rs.getString(i));
                }
                rows.add(new RowData(rowValues));
            }

            tableView.setItems(rows);

        } catch (SQLException e) {
            showAlert("SQL Error: " + e.getMessage());
        }
    }

    void deleteSelectedRecords() {
        String tableName = tableNameField.getText().trim();
        String idColumn = idColumnField.getText().trim();

        if (tableName.isEmpty() || idColumn.isEmpty()) {
            showAlert("Please fill in Table Name and ID Column.");
            return;
        }

        int idColIndex = columnNames.indexOf(idColumn);
        if (idColIndex == -1) {
            showAlert("ID Column not found in the table.");
            return;
        }

        ObservableList<RowData> rows = tableView.getItems();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            for (RowData row : rows) {
                if (row.isSelected()) {
                    String idValue = row.getValues().get(idColIndex);
                    String query = "DELETE FROM " + tableName + " WHERE " + idColumn + " = '" + idValue + "'";
                    stmt.executeUpdate(query);
                }
            }

            fetchRecords();

        } catch (SQLException e) {
            showAlert("SQL Error: " + e.getMessage());
        }
    }

    void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static class RowData {
        private final List<String> values;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);

        public RowData(List<String> values) {
            this.values = values;
        }

        public List<String> getValues() {
            return values;
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }
    }
}
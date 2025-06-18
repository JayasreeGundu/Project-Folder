import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public class JavaFXDBApp1 extends Application {
    // MySQL connection parameters - change these as per your MySQL setup
    private static final String MYSQL_HOST = "localhost";
    private static final int MYSQL_PORT = 3306;
    private static final String MYSQL_DB = "jayasree";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "root";

    private static final String DB_URL = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", MYSQL_HOST, MYSQL_PORT, MYSQL_DB);

    private Connection connection;

    private VBox mainContainer;
    private Button mainOperationButton;

    private enum Operation {
        CREATE,
        INSERT,
        UPDATE,
        DELETE,
        DROP
    }

    private Operation currentOperation;

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        setupDatabase();

        mainContainer = new VBox(16);
        mainContainer.setPadding(new Insets(24));
        mainContainer.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("MySQL Database Operations");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setPadding(new Insets(0, 0, 16, 0));
        titleLabel.setStyle("-fx-text-fill: #1f2937;");

        mainOperationButton = new Button("Select Operation");
        mainOperationButton.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        mainOperationButton.setPrefWidth(200);
        mainOperationButton.setOnAction(e -> showOperationSelectionDialog());

        mainContainer.getChildren().addAll(titleLabel, mainOperationButton);

        Scene scene = new Scene(mainContainer, 700, 600);
        scene.getRoot().setStyle("-fx-background-color: #f9fafb;");

        stage.setTitle("JavaFX MySQL Database Manager");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(600);
        stage.show();

        stage.setOnCloseRequest(this::closeApp);
    }

    private void closeApp(WindowEvent event) {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException ex) {
            // ignore
        }
    }

    private void setupDatabase() {
        try {
            // Load MySQL JDBC driver explicitly (optional for modern JDBC)
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, MYSQL_USER, MYSQL_PASSWORD);
        } catch (ClassNotFoundException e) {
            showError("Driver Error", "MySQL JDBC Driver not found. Please add it to your classpath.");
            Platform.exit();
        } catch (SQLException ex) {
            showError("Database connection failed", ex.getMessage());
            Platform.exit();
        }
    }

    private void showOperationSelectionDialog() {
        Dialog<Operation> dialog = new Dialog<>();
        dialog.initOwner(primaryStage);
        dialog.setTitle("Select Operation");
        dialog.setHeaderText("Please select the operation you want to perform.");

        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType);

        VBox dialogContent = new VBox(12);
        dialogContent.setPadding(new Insets(10));
        dialogContent.setAlignment(Pos.CENTER_LEFT);

        Operation[] operations = Operation.values();
        ToggleGroup toggleGroup = new ToggleGroup();

        for (Operation op : operations) {
            RadioButton rb = new RadioButton(op.name().substring(0,1) + op.name().substring(1).toLowerCase());
            rb.setToggleGroup(toggleGroup);
            rb.setUserData(op);
            rb.setFont(Font.font("Segoe UI", 15));
            dialogContent.getChildren().add(rb);
        }

        dialog.getDialogPane().setContent(dialogContent);

        Button okButton = new Button("OK");
        okButton.setDisable(true);
        okButton.setOnAction(e -> {
            RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
            if (selected != null) {
                currentOperation = (Operation) selected.getUserData();
                dialog.setResult(currentOperation);
                dialog.close();
            }
        });
        dialogContent.getChildren().add(okButton);

        toggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            okButton.setDisable(newToggle == null);
        });

        dialog.showAndWait().ifPresent(operation -> {
            mainContainer.getChildren().clear();
            mainContainer.getChildren().add(createBackButton());
            switch (operation) {
                case CREATE:
                    showCreateTableUI();
                    break;
                case INSERT:
                    showInsertUI();
                    break;
                case UPDATE:
                    showUpdateUI();
                    break;
                case DELETE:
                    showDeleteUI();
                    break;
                case DROP:
                    showDropUI();
                    break;
            }
        });
    }

    private Button createBackButton() {
        Button back = new Button("← Back to operations");
        back.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        back.setOnAction(e -> {
            mainContainer.getChildren().clear();
            mainContainer.getChildren().addAll(
                    new Label("MySQL Database Operations"){{
                        setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
                        setPadding(new Insets(0,0,16,0));
                        setStyle("-fx-text-fill: #1f2937;");
                    }},
                    mainOperationButton
            );
        });
        return back;
    }

    private void showCreateTableUI() {
        Label header = new Label("Create Table");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        header.setTextFill(javafx.scene.paint.Color.web("#111827"));

        VBox container = new VBox(12);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10,0,0,4); -fx-background-radius: 12;");
        container.setMaxWidth(600);

        TextField tableNameField = new TextField();
        tableNameField.setPromptText("Table Name");
        tableNameField.setFont(Font.font("Segoe UI", 14));

        Label columnsLabel = new Label("Columns (name type, e.g., id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL)");
        columnsLabel.setFont(Font.font("Segoe UI", 12));
        columnsLabel.setTextFill(javafx.scene.paint.Color.web("#4b5563"));

        TextArea columnsArea = new TextArea();
        columnsArea.setPromptText("Enter column definitions separated by commas");
        columnsArea.setFont(Font.font("Segoe UI", 14));
        columnsArea.setPrefRowCount(4);

        Button createBtn = new Button("Create Table");
        createBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        createBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-background-radius: 10;");
        createBtn.setOnMouseEntered(e -> createBtn.setStyle("-fx-background-color: #4338ca; -fx-text-fill: white; -fx-background-radius: 10;"));
        createBtn.setOnMouseExited(e -> createBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-background-radius: 10;"));

        createBtn.setOnAction(e -> {
            String tableName = tableNameField.getText().trim();
            String columnsText = columnsArea.getText().trim();

            if(tableName.isEmpty()) {
                showError("Validation Error", "Table name cannot be empty.");
                return;
            }

            if(columnsText.isEmpty()) {
                showError("Validation Error", "Columns definition cannot be empty.");
                return;
            }

            try {
                String createSQL = buildCreateTableSQL(tableName, columnsText);
                executeUpdate(createSQL);
                showInfo("Success", "Table '" + tableName + "' created successfully.");
                tableNameField.clear();
                columnsArea.clear();
            } catch (SQLException ex) {
                showError("SQL Error", ex.getMessage());
            }
        });

        container.getChildren().addAll(header, tableNameField, columnsLabel, columnsArea, createBtn);

        mainContainer.getChildren().add(container);
    }

    private String buildCreateTableSQL(String tableName, String columnsText) {
        // For MySQL, use backticks for identifiers
        return "CREATE TABLE IF NOT EXISTS " + tableName.replace("", "`") + " (" + columnsText + ");";
    }

    private void executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private List<String> getTables() {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getTables(MYSQL_DB, null, "%", new String[]{"TABLE"})) {
            while(rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tables.add(tableName);
            }
        } catch (SQLException ex) {
            showError("Database Error", ex.getMessage());
        }
        return tables;
    }

    private void showInsertUI() {
        Label header = new Label("Insert Into Table");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        header.setTextFill(javafx.scene.paint.Color.web("#111827"));

        VBox container = new VBox(16);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10,0,0,4); -fx-background-radius: 12;");
        container.setMaxWidth(700);

        Label selectTableLabel = new Label("Select Table:");
        selectTableLabel.setFont(Font.font("Segoe UI", 14));

        ObservableList<String> tablesList = FXCollections.observableArrayList(getTables());
        if(tablesList.isEmpty()) {
            Label noTables = new Label("No tables found in the database.");
            noTables.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
            container.getChildren().addAll(header, noTables);
            mainContainer.getChildren().add(container);
            return;
        }

        ComboBox<String> tablesComboBox = new ComboBox<>(tablesList);
        tablesComboBox.setPrefWidth(300);
        tablesComboBox.getSelectionModel().selectFirst();

        VBox formContainer = new VBox(12);

        Button insertBtn = new Button("Insert");
        insertBtn.setDisable(true);
        insertBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        insertBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 10;");
        insertBtn.setOnMouseEntered(e -> insertBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-background-radius: 10;"));
        insertBtn.setOnMouseExited(e -> insertBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 10;"));

        formContainer.setPadding(new Insets(12, 0, 0, 0));

        container.getChildren().addAll(header, selectTableLabel, tablesComboBox, formContainer, insertBtn);

        tablesComboBox.setOnAction(e -> {
            formContainer.getChildren().clear();
            String selectedTable = tablesComboBox.getSelectionModel().getSelectedItem();
            if (selectedTable != null) {
                generateInsertForm(selectedTable, formContainer, insertBtn);
            }
        });

        generateInsertForm(tablesComboBox.getSelectionModel().getSelectedItem(), formContainer, insertBtn);

        insertBtn.setOnAction(e -> {
            String table = tablesComboBox.getSelectionModel().getSelectedItem();
            if (table == null) return;
            try {
                insertDataFromForm(table, formContainer);
            } catch (Exception ex) {
                showError("Insert Error", ex.getMessage());
            }
        });

        mainContainer.getChildren().add(container);
    }

    private void generateInsertForm(String tableName, VBox formContainer, Button submitButton) {
        formContainer.getChildren().clear();

        List<ColumnInfo> columns = getTableColumns(tableName);
        if (columns.isEmpty()) {
            formContainer.getChildren().add(new Label("No columns found in table."));
            submitButton.setDisable(true);
            return;
        }

        List<TextField> textFields = new ArrayList<>();

        for (ColumnInfo column : columns) {
            Label label = new Label(column.name + " (" + column.type + ")");
            label.setFont(Font.font("Segoe UI", 14));
            TextField tf = new TextField();
            tf.setPromptText("Enter " + column.name);
            tf.setFont(Font.font("Segoe UI", 14));
            formContainer.getChildren().addAll(label, tf);
            textFields.add(tf);

            tf.textProperty().addListener((obs, oldVal, newVal) -> {
                boolean allFilled = textFields.stream().allMatch(field -> !field.getText().trim().isEmpty());
                submitButton.setDisable(!allFilled);
            });
        }
        submitButton.setDisable(true);
    }

    private static class ColumnInfo {
        String name;
        String type;

        ColumnInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    private List<ColumnInfo> getTableColumns(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        // Query MySQL INFORMATION_SCHEMA for columns metadata
        String sql = "SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, MYSQL_DB);
            pstmt.setString(2, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    String type = rs.getString("COLUMN_TYPE");
                    columns.add(new ColumnInfo(name, type));
                }
            }
        } catch (SQLException ex) {
            showError("Database Error", ex.getMessage());
        }
        return columns;
    }

    private void insertDataFromForm(String tableName, VBox formContainer) throws SQLException {
        List<ColumnInfo> columns = getTableColumns(tableName);
        List<String> values = new ArrayList<>();

        for (Node node : formContainer.getChildren()) {
            if (node instanceof TextField) {
                TextField tf = (TextField) node;
                String val = tf.getText().trim();
                if (val.isEmpty()) {
                    showError("Validation Error", "All fields are required.");
                    return;
                }
                values.add(val);
            }
        }

        if (values.size() != columns.size()) {
            showError("Internal Error", "Mismatch between inputs and columns.");
            return;
        }

        StringBuilder placeholders = new StringBuilder();
        StringBuilder columnNames = new StringBuilder();

        for (int i = 0; i < columns.size(); i++) {
            columnNames.append("").append(columns.get(i).name).append("");
            placeholders.append("?");
            if (i < columns.size() - 1) {
                columnNames.append(", ");
                placeholders.append(", ");
            }
        }

        String insertSQL = "INSERT INTO " + tableName.replace("", "`") + " (" + columnNames + ") VALUES (" + placeholders + ");";

        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setString(i + 1, values.get(i));
            }
            pstmt.executeUpdate();
            showInfo("Success", "Data inserted into '" + tableName + "' successfully.");
            for (Node node : formContainer.getChildren()) {
                if (node instanceof TextField) {
                    ((TextField) node).clear();
                }
            }
        }
    }

    private void showUpdateUI() {
        Label header = new Label("Update Table Data");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        header.setTextFill(javafx.scene.paint.Color.web("#111827"));

        VBox container = new VBox(16);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10,0,0,4); -fx-background-radius: 12;");
        container.setMaxWidth(700);

        Label selectTableLabel = new Label("Select Table:");
        selectTableLabel.setFont(Font.font("Segoe UI", 14));

        ObservableList<String> tablesList = FXCollections.observableArrayList(getTables());
        if(tablesList.isEmpty()) {
            Label noTables = new Label("No tables found in the database.");
            noTables.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
            container.getChildren().addAll(header, noTables);
            mainContainer.getChildren().add(container);
            return;
        }

        ComboBox<String> tablesComboBox = new ComboBox<>(tablesList);
        tablesComboBox.setPrefWidth(300);
        tablesComboBox.getSelectionModel().selectFirst();

        VBox formContainer = new VBox(12);

        Button loadDataBtn = new Button("Load Data");
        loadDataBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        loadDataBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 10;");
        loadDataBtn.setOnMouseEntered(e -> loadDataBtn.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 10;"));
        loadDataBtn.setOnMouseExited(e -> loadDataBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 10;"));

        TableView<ObservableList<String>> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setStyle("-fx-font-size: 14px;");
        tableView.setFixedCellSize(30);

        // Wrap the TableView in ScrollPane with max height for better data visibility
        ScrollPane scrollPane = new ScrollPane(tableView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setMaxHeight(350);
        scrollPane.setStyle("-fx-background: transparent;");

        loadDataBtn.setOnAction(e -> {
            String table = tablesComboBox.getSelectionModel().getSelectedItem();
            if(table != null) {
                loadTableData(tableView, table, formContainer);
            }
        });

        container.getChildren().addAll(header, selectTableLabel, tablesComboBox, loadDataBtn, scrollPane, formContainer);

        mainContainer.getChildren().add(container);
    }

    private void loadTableData(TableView<ObservableList<String>> tableView, String tableName, VBox formContainer) {
    tableView.getItems().clear();
    tableView.getColumns().clear();
    formContainer.getChildren().clear();

    List<ColumnInfo> columns = getTableColumns(tableName);
    if (columns.isEmpty()) {
        showError("Error", "No columns found in table.");
        return;
    }

    // Create columns in the TableView
    for (ColumnInfo column : columns) {
        final int colIndex = columns.indexOf(column);
        TableColumn<ObservableList<String>, String> col = new TableColumn<>(column.name);
        col.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().get(colIndex)));
        
        // Set a preferred width for the column
        col.setPrefWidth(150); // You can adjust this value based on your needs
        col.setMinWidth(100); // Minimum width to prevent it from collapsing
        col.setMaxWidth(300); // Maximum width to prevent it from expanding too much

        tableView.getColumns().add(col);
    }

    ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();

    // Retrieve data from the database
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName.replace("", "`") + "")) {
        while (rs.next()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (ColumnInfo col : columns) {
                String value = rs.getString(col.name);
                row.add(value == null ? "" : value);
            }
            data.add(row);
        }
    } catch (SQLException ex) {
        showError("Error loading data", ex.getMessage());
        return;
    }

    tableView.setItems(data);

    // Alternating row colors for better visibility
    tableView.setRowFactory(tv -> new TableRow<ObservableList<String>>() {
        @Override
        protected void updateItem(ObservableList<String> item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                setStyle("");
            } else {
                setStyle(getIndex() % 2 == 0 ? "-fx-background-color: #ffffff;" : "-fx-background-color: #f0f0f0;");
            }
        }
    });

    // Add listener for row selection
    tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
        formContainer.getChildren().clear();
        if (newSelection == null) return;

        Label instructions = new Label("Edit fields and press Update");
        instructions.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        instructions.setTextFill(javafx.scene.paint.Color.web("#2563eb"));

        List<TextField> fieldEditors = new ArrayList<>();

        for (int i = 0; i < columns.size(); i++) {
            Label label = new Label(columns.get(i).name);
            label.setFont(Font.font("Segoe UI", 13));
            TextField tf = new TextField(newSelection.get(i));
            tf.setFont(Font.font("Segoe UI", 14));
            formContainer.getChildren().addAll(label, tf);
            fieldEditors.add(tf);
        }
        Button updateBtn = new Button("Update");
        updateBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        updateBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 10;");
        updateBtn.setOnMouseEntered(e -> updateBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 10;"));
        updateBtn.setOnMouseExited(e -> updateBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 10;"));

        updateBtn.setOnAction(ev -> {
            try {
                updateTableRow(tableName, columns, newSelection, fieldEditors);
                loadTableData(tableView, tableName, formContainer);
                showInfo("Success", "Row updated successfully.");
            } catch (Exception ex) {
                showError("Update Failed", ex.getMessage());
            }
        });

        formContainer.getChildren().add(0, instructions);
        formContainer.getChildren().add(updateBtn);
    });
}


    private void updateTableRow(String tableName, List<ColumnInfo> columns, ObservableList<String> oldRow, List<TextField> editors) throws SQLException {
        if (oldRow.size() != editors.size())
            throw new SQLException("Mismatch in columns and editors");

        StringBuilder setClause = new StringBuilder();
        StringBuilder whereClause = new StringBuilder();
        List<String> setValues = new ArrayList<>();
        List<String> whereValues = new ArrayList<>();

        // We use primary key(s) for WHERE clause; if none, fallback to all columns
        List<String> primaryKeys = getPrimaryKeys(tableName);

        if (primaryKeys.isEmpty()) {
            // No PK, use all columns as WHERE
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) setClause.append(", ");
                setClause.append("").append(columns.get(i).name).append(" = ?");
                setValues.add(editors.get(i).getText().trim());
            }
            whereClause.append(" WHERE ");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) whereClause.append(" AND ");
                whereClause.append("").append(columns.get(i).name).append(" = ?");
                whereValues.add(oldRow.get(i));
            }
        } else {
            // Only update columns that are not primary keys
            boolean firstSet = true;
            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).name;
                if (!primaryKeys.contains(colName)) {
                    if (!firstSet) setClause.append(", ");
                    setClause.append("").append(colName).append(" = ?");
                    setValues.add(editors.get(i).getText().trim());
                    firstSet = false;
                }
            }
            whereClause.append(" WHERE ");
            boolean firstWhere = true;
            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).name;
                if (primaryKeys.contains(colName)) {
                    if (!firstWhere) whereClause.append(" AND ");
                    whereClause.append("").append(colName).append(" = ?");
                    whereValues.add(oldRow.get(i));
                    firstWhere = false;
                }
            }
        }

        if (setClause.length() == 0 || whereValues.isEmpty()) {
            throw new SQLException("Cannot update: No columns to set or no primary key for WHERE clause.");
        }

        String sql = "UPDATE " + tableName.replace("", "`") + " SET " + setClause + whereClause + ";";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int index = 1;
            for (String val : setValues) {
                pstmt.setString(index++, val);
            }
            for (String val : whereValues) {
                pstmt.setString(index++, val);
            }
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No rows updated. Possibly no matching row found.");
            }
        }
    }

    private List<String> getPrimaryKeys(String tableName) {
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getPrimaryKeys(MYSQL_DB, null, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        } catch (SQLException ex) {
            showError("Database Error", "Failed to retrieve primary keys: " + ex.getMessage());
        }
        return primaryKeys;
    }

    private void showDeleteUI() {
        Label header = new Label("Delete Table Data");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        header.setTextFill(javafx.scene.paint.Color.web("#111827"));

        VBox container = new VBox(16);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10,0,0,4); -fx-background-radius: 12;");
        container.setMaxWidth(700);

        Label selectTableLabel = new Label("Select Table:");
        selectTableLabel.setFont(Font.font("Segoe UI", 14));

        ObservableList<String> tablesList = FXCollections.observableArrayList(getTables());
        if(tablesList.isEmpty()) {
            Label noTables = new Label("No tables found in the database.");
            noTables.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
            container.getChildren().addAll(header, noTables);
            mainContainer.getChildren().add(container);
            return;
        }

        ComboBox<String> tablesComboBox = new ComboBox<>(tablesList);
        tablesComboBox.setPrefWidth(300);
        tablesComboBox.getSelectionModel().selectFirst();

        TableView<ObservableList<String>> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setStyle("-fx-font-size: 14px;");
        tableView.setFixedCellSize(30);

        // Wrap the TableView in ScrollPane with max height for better data visibility
        ScrollPane scrollPane = new ScrollPane(tableView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setMaxHeight(350);
        scrollPane.setStyle("-fx-background: transparent;");

        Button deleteRowBtn = new Button("Delete Selected Row");
        deleteRowBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        deleteRowBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 10;");
        deleteRowBtn.setDisable(true);
        deleteRowBtn.setOnMouseEntered(e -> {
            if (!deleteRowBtn.isDisabled()) deleteRowBtn.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white; -fx-background-radius: 10;");
        });
        deleteRowBtn.setOnMouseExited(e -> {
            if (!deleteRowBtn.isDisabled()) deleteRowBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 10;");
        });

        Button loadDataBtn = new Button("Load Data");
        loadDataBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        loadDataBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 10;");
        loadDataBtn.setOnMouseEntered(e -> loadDataBtn.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 10;"));
        loadDataBtn.setOnMouseExited(e -> loadDataBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 10;"));

        loadDataBtn.setOnAction(e -> {
            String table = tablesComboBox.getSelectionModel().getSelectedItem();
            if(table != null) {
                loadTableData(tableView, table, new VBox());
            }
            deleteRowBtn.setDisable(true);
        });

        tablesComboBox.setOnAction(e -> {
            tableView.getItems().clear();
            tableView.getColumns().clear();
            deleteRowBtn.setDisable(true);
        });

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            deleteRowBtn.setDisable(newSel == null);
        });

        deleteRowBtn.setOnAction(e -> {
            ObservableList<String> selected = tableView.getSelectionModel().getSelectedItem();
            String table = tablesComboBox.getSelectionModel().getSelectedItem();
            if(selected == null || table == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Deletion");
            confirm.setHeaderText("Are you sure you want to delete the selected row?");
            confirm.initOwner(primaryStage);
            Optional<ButtonType> result = confirm.showAndWait();
            if(result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    deleteTableRow(table, selected);
                    showInfo("Success", "Row deleted successfully.");
                    loadTableData(tableView, table, new VBox());
                    deleteRowBtn.setDisable(true);
                } catch (SQLException ex) {
                    showError("Delete Error", ex.getMessage());
                }
            }
        });

        container.getChildren().addAll(header, selectTableLabel, tablesComboBox, loadDataBtn, scrollPane, deleteRowBtn);

        mainContainer.getChildren().add(container);
    }

    private void deleteTableRow(String tableName, ObservableList<String> row) throws SQLException {
        List<ColumnInfo> columns = getTableColumns(tableName);
        if(columns.size() != row.size()) {
            throw new SQLException("Column and row size mismatch.");
        }

        List<String> primaryKeys = getPrimaryKeys(tableName);
        if (primaryKeys.isEmpty()) {
            throw new SQLException("Cannot delete row without primary key identification.");
        }

        StringBuilder whereClause = new StringBuilder();
        List<String> whereValues = new ArrayList<>();

        whereClause.append(" WHERE ");
        boolean firstWhere = true;
        for (int i = 0; i < columns.size(); i++) {
            String colName = columns.get(i).name;
            if(primaryKeys.contains(colName)) {
                if (!firstWhere) whereClause.append(" AND ");
                whereClause.append("").append(colName).append(" = ?");
                whereValues.add(row.get(i));
                firstWhere = false;
            }
        }

        if(whereValues.isEmpty()) {
            throw new SQLException("No primary key values found for deletion.");
        }

        String sql = "DELETE FROM " + tableName.replace("", "`") + "" + whereClause + ";";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for(int i = 0; i < whereValues.size(); i++) {
                pstmt.setString(i + 1, whereValues.get(i));
            }
            int affected = pstmt.executeUpdate();
            if(affected == 0) {
                throw new SQLException("No rows deleted. Possibly no matching row found.");
            }
        }
    }

    private void showDropUI() {
        Label header = new Label("Drop Table");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        header.setTextFill(javafx.scene.paint.Color.web("#111827"));

        VBox container = new VBox(16);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10,0,0,4); -fx-background-radius: 12;");
        container.setMaxWidth(400);

        Label selectTableLabel = new Label("Select Table to Drop:");
        selectTableLabel.setFont(Font.font("Segoe UI", 14));

        ObservableList<String> tablesList = FXCollections.observableArrayList(getTables());
        if(tablesList.isEmpty()) {
            Label noTables = new Label("No tables found in the database.");
            noTables.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
            container.getChildren().addAll(header, noTables);
            mainContainer.getChildren().add(container);
            return;
        }

        ComboBox<String> tablesComboBox = new ComboBox<>(tablesList);
        tablesComboBox.setPrefWidth(300);
        tablesComboBox.getSelectionModel().selectFirst();

        Button dropBtn = new Button("Drop Table");
        dropBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        dropBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 10;");
        dropBtn.setOnMouseEntered(e -> dropBtn.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white; -fx-background-radius: 10;"));
        dropBtn.setOnMouseExited(e -> dropBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 10;"));

        dropBtn.setOnAction(e -> {
            String table = tablesComboBox.getSelectionModel().getSelectedItem();
            if (table == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Drop Table");
            confirm.setHeaderText("Are you sure you want to drop the table '" + table + "'? This action cannot be undone.");
            confirm.initOwner(primaryStage);
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    executeUpdate("DROP TABLE IF EXISTS " + table.replace("", "`") + ";");
                    showInfo("Success", "Table '" + table + "' dropped successfully.");
                    tablesList.setAll(getTables());
                    if(tablesList.isEmpty()) {
                        mainContainer.getChildren().clear();
                        mainContainer.getChildren().add(createBackButton());
                        mainContainer.getChildren().add(new Label("No tables present in the database."));
                    }
                } catch (SQLException ex) {
                    showError("Drop Table Error", ex.getMessage());
                }
            }
        });

        container.getChildren().addAll(header, selectTableLabel, tablesComboBox, dropBtn);

        mainContainer.getChildren().add(container);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
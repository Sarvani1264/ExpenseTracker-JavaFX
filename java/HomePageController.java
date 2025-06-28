package com.example.expensetracker;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import static com.example.expensetracker.SceneController.showError;

public class HomePageController {
    @FXML private Label dis_username;
    @FXML BarChart<String,Number> exp_barchart;
    @FXML private Label category, amount, title, newpwdLabel, incorrectLabel, successmsg;
    @FXML private PieChart pieChart;
    @FXML private ChoiceBox<String> pick_category, selectType,selectMonth;
    @FXML private ComboBox<Integer> selectYear;
    @FXML private DatePicker pick_date;
    @FXML private TextField amt, newpwd, checkpwd;
    @FXML private TableView<Expense> table;
    @FXML private TableColumn<Expense, Integer> col_amount;
    @FXML private TableColumn<Expense, String> col_category;
    @FXML private TableColumn<Expense, String> dateTime;
    @FXML private TableColumn<Expense, Void> buttons;
    @FXML private Button add, changepwd, exit;
    @FXML private AnchorPane pwdpane;

    private final Connection con = database.connectDb();;
    private PreparedStatement ps;
    private ResultSet rs;

    private int userid;
    private String uname;
    public void display(String username,int id){
        this.uname = dis_username.getText();
        dis_username.setText(username);
        this.userid=id;

        loadChart();
    }

    public void loadChart(){
        LocalDateTime today = LocalDateTime.now();
        int present_month = today.getMonthValue();
        int present_year = today.getYear();
        int maxAmount = 0;
        String sql = "SELECT DAY(date_time) AS day,SUM(Amount_spent) AS amount FROM expenses WHERE MONTH(date_time) = ? AND YEAR(date_time)=? AND user_id=? GROUP BY DAY(date_time) ORDER BY DAY(date_time)";
        try{
            ps= con.prepareStatement(sql);
            ps.setInt(1,present_month);
            ps.setInt(2,present_year);
            ps.setInt(3,userid);
            rs= ps.executeQuery();

            XYChart.Series<String,Number> series = new XYChart.Series<>();
            while(rs.next()){
                String day=String.valueOf(rs.getInt("day"));
                int amount= rs.getInt("amount");
                maxAmount = Math.max(maxAmount, amount);
                series.getData().add(new XYChart.Data<>(day,amount));
            }
            exp_barchart.getData().clear();
            exp_barchart.getData().add(series);

            NumberAxis yAxis = (NumberAxis) exp_barchart.getYAxis();
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(0);
            yAxis.setUpperBound((Math.ceil((maxAmount+1000) / 1000.0)) * 1000);
            yAxis.setTickUnit(1000);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void toHome(){
        loadChart();
        SceneController.setVisibleNodes(false,category,amount,amt,pick_category,table,add,selectMonth,selectYear,pick_date,pieChart,selectType,pwdpane,changepwd,exit);
        SceneController.setVisibleNodes(true,exp_barchart);
        dis_username.setText(uname);
    }


    @FXML
    public void initialize(){
        pick_category.getItems().addAll("Food","Transport","Housing","Health","Entertainment","Education","Debt","Personal","Others","Savings");
        pick_category.setValue("Food");
        selectType.getItems().addAll("Today","Day","Month","Year");
        selectType.setValue("Today");
        selectMonth.getItems().addAll("January","February","March","April","May","June","July","August","September","October","November","December");
        selectMonth.setValue(String.valueOf(LocalDate.now().getMonth()));
        for(int i =java.time.Year.now().getValue();i>=2000;i--){
            selectYear.getItems().add(i);
        }
        selectYear.setValue(LocalDate.now().getYear());

        dateTime.setCellValueFactory(data -> data.getValue().dateProperty());
        col_category.setCellValueFactory(data -> data.getValue().categoryProperty());
        col_amount.setCellValueFactory(data -> data.getValue().amountProperty().asObject());
        buttons.setCellFactory(col -> new TableCell<>(){
            private final Button delete = new Button("Delete");
            {
                delete.setStyle("-fx-background-color:red");
                delete.setOnAction(event -> {Expense exp = getTableView().getItems().get(getIndex());
                    removeExp(exp);
                    loadExpenses(userid);});
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(delete);
                }
            }
            });

        table.setItems(data);

        selectType.getSelectionModel().selectedItemProperty().addListener((obs,oldval,newval) -> {loadPieChart(selectType.getValue());});
        pick_date.valueProperty().addListener((obs, oldDate, newDate) -> {loadPieChart(selectType.getValue());});
        selectMonth.valueProperty().addListener((obs, oldMonth, newMonth) -> {loadPieChart(selectType.getValue());});
        selectYear.valueProperty().addListener((obs, oldYear, newYear) -> {loadPieChart(selectType.getValue());});

    }

    public void addExpense() {
        SceneController.setVisibleNodes(true,category,amount,amt,pick_category,table,add);
        SceneController.setVisibleNodes(false,exp_barchart,selectType,selectMonth,selectYear,pick_date,pieChart);

        loadExpenses(userid);
    }

    public static class Expense{
        private final LocalDateTime dateTime;
        private final SimpleStringProperty col_category;
        private final SimpleIntegerProperty col_amount;

        public Expense(LocalDateTime dateTime, String category, Number amount){
            this.dateTime= dateTime;
            this.col_category =new SimpleStringProperty(category);
            this.col_amount =new SimpleIntegerProperty((Integer) amount);
        }

        public SimpleStringProperty dateProperty() {
            String formatted = dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a"));
            return new SimpleStringProperty(formatted);
        }

        public SimpleStringProperty categoryProperty() {
            return col_category;
        }

        public SimpleIntegerProperty amountProperty() {
            return col_amount;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

    }
    private ObservableList<Expense> data = FXCollections.observableArrayList();

    public void loadExpenses(int userid){
        title.setText("Today's Expenses");
        String sql = "SELECT * FROM expenses WHERE DATE(date_time)=? AND user_id=?";
        try{
            ps = con.prepareStatement(sql);
            ps.setDate(1,java.sql.Date.valueOf(LocalDate.now()));
            ps.setInt(2,userid);
            rs=ps.executeQuery();
            data.clear();
            while(rs.next()){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime adddate = LocalDateTime.parse(rs.getString("date_time"), formatter);
                String addcat = rs.getString("expense_type");
                int addAmount = rs.getInt("amount_spent");
                data.add(new Expense(adddate,addcat,addAmount));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeExp(Expense exp){
        String sql="SELECT * FROM expenses WHERE date_time =? AND user_id=?";
        String sql2="DELETE FROM expenses WHERE exp_id=?";
        try {
            ps=con.prepareStatement(sql);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String dt = (exp.getDateTime().format(formatter));
            ps.setTimestamp(1, Timestamp.valueOf(dt));
            ps.setInt(2,userid);
            rs=ps.executeQuery();
            while(rs.next()){
                int expid=rs.getInt("exp_id");
                ps=con.prepareStatement(sql2);
                ps.setInt(1,expid);
                ps.executeUpdate();
                ps.close();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addExp(ActionEvent event) {
        String datetime= LocalDate.now() +" "+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String cat=pick_category.getValue();
        int amount=0;
        if(amt.getText().isEmpty()){
           SceneController.showError("Please enter the Amount.");
           return;
        }else{
            try {
                amount = Integer.parseInt(amt.getText());
                if (amount <= 0) {
                    SceneController.showError("Amount must be a positive number.");
                    return;
                }
            } catch (NumberFormatException ex) {
                SceneController.showError("Amount must be a number.");
                return;
            }
        }
        String sql="INSERT INTO expenses (user_id,date_time,expense_type,amount_spent) VALUES (?,?,?,?)";
        try {
            ps= con.prepareStatement(sql);
            ps.setInt(1,userid);
            ps.setTimestamp(2,Timestamp.valueOf(datetime));
            ps.setString(3,cat);
            ps.setInt(4, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        loadExpenses(userid);
    }

    public void showExpenses(ActionEvent event) {
        title.setText("Categorical View");
        SceneController.setVisibleNodes(false,category,amount,amt,pick_category,table,add,exp_barchart,selectYear,selectMonth,pick_date);
        SceneController.setVisibleNodes(true,selectType,pieChart);
        selectType.setValue("Today");
        loadPieChart(selectType.getValue());
    }


    private void loadPieChart(String selection){
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        String sql="";
        LocalDate parameter1=null;
        int parameter2 =0;
        switch (selection){
            case "Today":
                sql ="SELECT expense_type,SUM(Amount_spent) AS Amount from expenses WHERE DATE(date_time)=? AND user_id=? GROUP BY expense_type";
                parameter1 = LocalDate.now();
                break;
            case "Day":
                SceneController.setVisibleNodes(false,selectMonth,selectYear);
                pick_date.setVisible(true);
                sql ="SELECT expense_type,SUM(Amount_spent) AS Amount from expenses WHERE DATE(date_time)=? AND user_id=? GROUP BY expense_type";
                parameter1=pick_date.getValue();
                break;
            case "Month":
                SceneController.setVisibleNodes(false,pick_date,selectYear);
                selectMonth.setVisible(true);
                sql ="SELECT expense_type,SUM(Amount_spent) AS Amount from expenses WHERE MONTH(date_time)=? AND user_id=? GROUP BY expense_type";
                parameter2= Month.valueOf(selectMonth.getValue().toUpperCase()).getValue();
                break;
            case "Year":
                SceneController.setVisibleNodes(false,pick_date,selectMonth);
                selectYear.setVisible(true);
                sql ="SELECT expense_type,SUM(Amount_spent) AS Amount from expenses WHERE YEAR(date_time)=? AND user_id=? GROUP BY expense_type";
                parameter2=selectYear.getValue();
                break;
        }
        try {
            ps = con.prepareStatement(sql);
            if(selection.equals("Today") || selection.equals("Day")) {
                ps.setDate(1, java.sql.Date.valueOf(parameter1));
            }else{
                ps.setInt(1,parameter2);
            }
            ps.setInt(2,userid);
            rs= ps.executeQuery();
            while (rs.next()){
                String type = rs.getString("expense_type");
                int money = rs.getInt("Amount");
                pieData.add(new PieChart.Data(type, money));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("No Data", 1));
        }
        pieChart.setData(pieData);
    }

    public void getReports(ActionEvent event) throws IOException {
        SceneController.loadController("ReportsAndSavings.fxml",event,(ReportsToSettingsController c) -> {c.display(uname,userid); c.loadReportChart();});
    }
    public void getSavings(ActionEvent event) throws IOException {
        SceneController.loadController("ReportsAndSavings.fxml",event,(ReportsToSettingsController c) -> {c.display(uname,userid); c.loadSavingsChart();});
    }
    @FXML
    private void openProfile(ActionEvent event) throws IOException {
        SceneController.setVisibleNodes(false,changepwd,exit);
        SceneController.loadController("ReportsAndSavings.fxml",event,(ReportsToSettingsController c) -> {c.display(uname,userid);c.setProfile(event);});
    }
    @FXML
    private void settings(ActionEvent e){
        SceneController.setVisibleNodes(true,changepwd,exit);
    }
    @FXML
    public void changePwd(ActionEvent event) throws IOException {
        SceneController.setVisibleNodes(true,pwdpane);
        SceneController.setVisibleNodes(false,successmsg,incorrectLabel,changepwd,exit);
        checkpwd.setOnKeyPressed(event1 -> {
            SceneController.setVisibleNodes(false,incorrectLabel);
            if(event1.getCode() == KeyCode.ENTER){
                String check = "SELECT password FROM user_data WHERE userId = ?";
                try {
                    ps = con.prepareStatement(check);
                    ps.setInt(1,userid);
                    rs = ps.executeQuery();
                    while(rs.next()){
                        if(rs.getString("password").equals(checkpwd.getText())){
                            updatePwd();
                        }else{
                            SceneController.setVisibleNodes(true,incorrectLabel);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                checkpwd.setText("");
                newpwd.setText("");
            }
        });
    }
    public void updatePwd(){
        SceneController.setVisibleNodes(true,newpwdLabel,newpwd);
        newpwd.setOnKeyPressed(ev -> {
            if(ev.getCode() == KeyCode.ENTER) {
                String chg = "UPDATE user_data SET password = ? WHERE userId =?";
                try {
                    ps = con.prepareStatement(chg);
                    ps.setString(1,newpwd.getText());
                    ps.setInt(2,userid);
                    ps.executeUpdate();
                    SceneController.setVisibleNodes(true,successmsg);
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(e -> {
                        SceneController.setVisibleNodes(false,newpwdLabel,newpwd,successmsg,pwdpane);
                    });
                    pause.play();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    public void logout(ActionEvent event) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Logout Confirmation");
        alert.setContentText("Do you really want to logout?");
        if(alert.showAndWait().get()==ButtonType.OK){
            SceneController.loadfxml("LogIn.fxml",event);
        }else{
            SceneController.setVisibleNodes(false,changepwd,exit);
        }
    }

}

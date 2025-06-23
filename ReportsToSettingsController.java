package com.example.expensetracker;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;

public class ReportsToSettingsController {
    @FXML private AreaChart<Number,Number> report_chart;
    @FXML private Label report_info, heading, dis_username, income, spent, saved, month_savings;
    @FXML private ChoiceBox<String> month_report;
    @FXML private LineChart<String,Number> SavingsChart;
    @FXML private AnchorPane showSavings, report, detailsPane, pwdpane;
    @FXML private Label p_email, p_salary, p_savgoal, p_username, chng_details, newpwdLabel, successmsg, incorrectLabel;
    @FXML private TextField changes, checkpwd, newpwd;
    @FXML private Button change_btn, pwdchng, exit;

    private final Connection con = database.connectDb();
    private PreparedStatement ps;
    private ResultSet rs;

    int userid;
    String uname;
    public void display(String username,int id){
        dis_username.setText(username);
        this.uname=username;
        this.userid=id;

    }


    public void initialize() {
        month_report.getItems().addAll("January","February","March","April","May","June","July","August","September","October","November","December");
        month_report.setValue(String.valueOf(LocalDate.now().getMonth()));
        loadReportChart();
        dis_username.setText(uname);
        month_report.valueProperty().addListener((obs, oldMonth, newMonth) -> {loadReportChart();});
    }

    public void toHome(ActionEvent event) throws IOException {
        SceneController.loadController("HomePage.fxml",event,(HomePageController c)->{c.display(uname,userid); c.toHome();});

        dis_username.setText(uname);
    }

    public void loadReportChart() {
        int savings=0;
        SceneController.setVisibleNodes(true, report_chart, report, heading);
        SceneController.setVisibleNodes(false,SavingsChart,showSavings, detailsPane);
        NumberAxis xAxis = (NumberAxis) report_chart.getXAxis();
        xAxis.setLabel("Day of month");
        xAxis.setLowerBound(1);
        xAxis.setUpperBound(31);
        xAxis.setTickUnit(1);

        NumberAxis yAxis = (NumberAxis) report_chart.getYAxis();
        yAxis.setLabel("Amount Spent");

        report_chart.getData().clear();
        String[] categories = {"Food", "Transport", "Housing", "Health", "Entertainment", "Education", "Debt", "Personal", "Others","Savings"};

        String sql = "SELECT DAY(date_time) AS day,SUM(Amount_spent) AS amount FROM expenses WHERE MONTH(date_time)=? AND expense_type=? AND user_id=? GROUP BY DAY(date_time) ORDER BY DAY(date_time)";
        try {
            for (String type : categories) {
                XYChart.Series series = new XYChart.Series<>();
                ps = con.prepareStatement(sql);
                ps.setInt(1, Month.valueOf(month_report.getValue().toUpperCase()).getValue());
                ps.setString(2, type);
                ps.setInt(3, userid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int day = rs.getInt("day");
                    int amt = rs.getInt("amount");
                    series.getData().add(new XYChart.Data<>(day, amt));
                    if(type.equals("Savings")){
                        savings += amt;
                    }
                }
                series.setName(type);
                report_chart.getData().add(series);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String sql1 = "SELECT salary FROM user_data WHERE userId = ?";
        int inc = 0;
        try {
            ps = con.prepareStatement(sql1);
            ps.setInt(1, userid);
            rs = ps.executeQuery();
            while (rs.next()) {
                inc = rs.getInt("salary");
                income.setText(String.valueOf(inc));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sql2 = "SELECT SUM(Amount_spent) AS amount FROM expenses WHERE MONTH(date_time)=? AND YEAR(date_time)=? AND user_id =?";
        int spt = 0;
        try {
            ps = con.prepareStatement(sql2);
            ps.setInt(1, Month.valueOf(month_report.getValue().toUpperCase()).getValue());
            ps.setInt(2, LocalDate.now().getYear());
            ps.setInt(3,userid);
            rs = ps.executeQuery();
            while (rs.next()) {
                spt = rs.getInt("amount");
                spent.setText(String.valueOf(spt));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        saved.setText(String.valueOf(savings));
        int savings_goal = 0;
        String getSavings = "SELECT savings_goal from user_data WHERE userId = ?";
        try {
            ps = con.prepareStatement(getSavings);
            ps.setInt(1,userid);
            rs =ps.executeQuery();
            while(rs.next()){
                savings_goal = rs.getInt("savings_goal");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String max_amt = "SELECT expense_type, SUM(Amount_spent) AS total_spent FROM expenses WHERE MONTH(date_time) = ? AND user_id=? GROUP BY expense_type ORDER BY total_spent DESC LIMIT 1";
        String expense = "";
        try{
            ps=con.prepareStatement(max_amt);
            ps.setInt(1,Month.valueOf(month_report.getValue().toUpperCase()).getValue());
            ps.setInt(2,userid);
            rs=ps.executeQuery();
            while (rs.next()){
                expense=rs.getString("expense_type");
                break;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if(Month.valueOf(month_report.getValue().toUpperCase()).equals(LocalDate.now().getMonth())){
            report_info.setText("Save "+(savings_goal-savings)+" to reach your savings goal.");
        }
        else {
            if (savings < savings_goal) {
                report_info.setText("Savings goal not achieved.Try saving money by reducing " + expense);
            } else {
                report_info.setText("You achieved your savings goal.");
            }
        }
    }

    public void loadSavingsChart() {
        heading.setText("Savings");
        SceneController.setVisibleNodes(false, report_chart, report,detailsPane);
        SceneController.setVisibleNodes(true,SavingsChart,showSavings);
        CategoryAxis xAxis = (CategoryAxis) SavingsChart.getXAxis();
        xAxis.setLabel("Months");

        NumberAxis yAxis = (NumberAxis) SavingsChart.getYAxis();
        yAxis.setLabel("Savings");

        SavingsChart.getData().clear();
        XYChart.Series series = new XYChart.Series<>();
        String savings = "SELECT MONTH(date_time) AS month,SUM(Amount_spent) AS amount FROM expenses WHERE expense_type=? AND  YEAR(date_time)=? AND user_id=? GROUP BY MONTH(date_time) ORDER BY MONTH(date_time)";
        try {
            ps = con.prepareStatement(savings);
            ps.setString(1, "Savings");
            ps.setInt(2, LocalDate.now().getYear());
            ps.setInt(3,userid);
            rs = ps.executeQuery();
            while (rs.next()) {
                String month = Month.of(rs.getInt("month")).name();
                int saved = rs.getInt("amount");
                series.getData().add(new XYChart.Data<>(month,saved));
                if(month.toUpperCase().equals(LocalDate.now().getMonth().name())){
                    month_savings.setText(String.valueOf(saved));
                }
            }
            SavingsChart.getData().add(series);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void setProfile(ActionEvent event){
        heading.setText("My Profile");
        SceneController.setVisibleNodes(false, report_chart, report, SavingsChart, month_report,showSavings,pwdchng,exit);
        SceneController.setVisibleNodes(true, detailsPane);
        String details = "SELECT * FROM user_data WHERE userId=?";
        try {
            ps = con.prepareStatement(details);
            ps.setInt(1,userid);
            rs=ps.executeQuery();
            while (rs.next()){
                p_username.setText("  "+rs.getString("username"));
                p_email.setText("  "+rs.getString("email_id"));
                p_salary.setText("  "+rs.getString("salary"));
                p_savgoal.setText("  "+rs.getString("savings_goal"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private String column="";
    @FXML
    void goal_edit(ActionEvent event) {
        SceneController.setVisibleNodes(true,chng_details,changes,change_btn);
        chng_details.setText("Enter your new goal:");
        column = "savings_goal";
    }
    @FXML
    void mail_edit(ActionEvent event) {
        SceneController.setVisibleNodes(true,chng_details,changes,change_btn);
        chng_details.setText("Enter your new Email:");
        column = "email_id";
    }
    @FXML
    void name_edit(ActionEvent event) {
        SceneController.setVisibleNodes(true,chng_details,changes,change_btn);
        chng_details.setText("Enter your new Username:");
        column = "username";
    }
    @FXML
    void sal_edit(ActionEvent event) {
        SceneController.setVisibleNodes(true,chng_details,changes,change_btn);
        chng_details.setText("Enter your new Salary:");
        column = "salary";
    }
    public void setChng_details(ActionEvent ev){
        if(changes.getText().isEmpty()){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error message");
            alert.setHeaderText(null);
            alert.setContentText("Please fill the details");
            alert.showAndWait();
        }else {
            String change = "UPDATE user_data SET "+column+"=? WHERE userId = ?";
            try {
                ps = con.prepareStatement(change);
                ps.setString(1,changes.getText());
                ps.setInt(2,userid);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        setProfile(ev);
        SceneController.setVisibleNodes(false,change_btn,changes,chng_details);
    }
    @FXML
    private void settings(ActionEvent event){
        SceneController.setVisibleNodes(true,pwdchng,exit);
    }
    @FXML
    private void changePwd(ActionEvent event) throws IOException {
        SceneController.loadController("HomePage.fxml",event,(HomePageController c)-> {c.display(uname,userid);c.changePwd(event);});
    }
    @FXML
    private void logout(ActionEvent event) throws IOException {
        SceneController.loadController("HomePage.fxml",event,(HomePageController c) -> {c.display(uname,userid);c.logout(event);});
    }
}

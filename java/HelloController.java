package com.example.expensetracker;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HelloController {

    private Connection con = database.connectDb();;
    private PreparedStatement ps;
    private ResultSet rs;
    String sql;
    private String user_name;
    private int user_id;



    @FXML private TextField uname ;
    @FXML private PasswordField pwd;
    @FXML private Label haveacc;
    Alert alert;
    public void logIn(ActionEvent event) throws SQLException, IOException {
        user_name = uname.getText();
        if(uname.getText().isEmpty() || pwd.getText().isEmpty()){
            SceneController.showError("Please fill the data.");
        }
        else{
            sql = "SELECT * FROM user_data WHERE username = ? AND password = ?";
            try{
                ps = con.prepareStatement(sql);
                ps.setString(1, uname.getText());
                ps.setString(2, pwd.getText());
                rs=ps.executeQuery();
                if (rs.next()) {
                    int salary = rs.getInt("salary");
                    user_id = rs.getInt("userId");
                    if(salary==0){
                        SceneController.loadfxml("userDetails.fxml",event);
                    }else {
                        SceneController.loadController("HomePage.fxml", event, (HomePageController c) -> c.display(user_name,user_id));
                    }
                } else {
                    haveacc.setText("Incorrect username or password.");
                }
            }catch (Exception e){e.printStackTrace();}
            finally {
                ps.close();
                rs.close();
            }
        }
    }


    @FXML TextField newuser;
    @FXML PasswordField userpwd;
    @FXML PasswordField cpwd;
    @FXML Label message;
    public void signUp(ActionEvent e) throws SQLException, IOException {
        String new_username = newuser.getText();

        if(newuser.getText().isEmpty() || userpwd.getText().isEmpty() || cpwd.getText().isEmpty()){
            SceneController.showError("Please fill the data.");
        }

        sql="SELECT * FROM user_data WHERE username=?";
        ps= con.prepareStatement(sql);
        try {
            ps.setString(1,new_username);
            rs=ps.executeQuery();
            if(rs.next()){
                message.setText("The username has already taken.");
            }else {
                if(userpwd.getText().equals(cpwd.getText())) {
                    sql = "INSERT INTO user_data (username,password) VALUES (?,?)";
                    ps= con.prepareStatement(sql);
                    try {
                        ps.setString(1, newuser.getText());
                        ps.setString(2, userpwd.getText());
                        ps.executeUpdate();
                        user_name = newuser.getText();
                    }catch (Exception ev){ev.printStackTrace();}
                    SceneController.loadfxml("userDetails.fxml",e);
                }else{
                    message.setText("Password and Confirm password should be same!!!");
                }
            }
        }catch (Exception ex){ex.printStackTrace();}
    }

    @FXML private Label filldetailsmsg;
    @FXML private TextField email;
    @FXML private TextField sal;
    @FXML private TextField savings;
    public void submit(ActionEvent e) throws SQLException, IOException {
        if(email.getText().isEmpty() || sal.getText().isEmpty() || savings.getText().isEmpty()){
            filldetailsmsg.setText("Please fill the empty fields.");
        }
        else {
            sql = "UPDATE user_data SET email_id=?, salary=?, savings_goal=? WHERE username=?";
            con = database.connectDb();

            int salary=Integer.parseInt(sal.getText());
            int saving=Integer.parseInt(savings.getText());
            try {
                ps = con.prepareStatement(sql);
                ps.setString(1, email.getText());
                ps.setInt(2, salary);
                ps.setInt(3, saving);
                ps.setString(4, user_name);
                ps.executeUpdate();
                ps.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            sql = "SELECT userId FROM user_data WHERE username=?";
            ps= con.prepareStatement(sql);
            try {
                ps.setString(1,user_name);
                rs=ps.executeQuery();
                if(rs.next()) {
                    user_id = rs.getInt("user_id");
                }
            }catch (Exception ev){ev.printStackTrace();}
            SceneController.loadController("HomePage.fxml",e,(HomePageController c)->c.display(user_name,user_id));
        }
    }

    public void logInPage(ActionEvent event) throws IOException {
        SceneController.loadfxml("LogIn.fxml",event);
    }
    public void signUpPage(ActionEvent event) throws IOException {
        SceneController.loadfxml("SignUp.fxml",event);
    }
}

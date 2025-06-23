package com.example.expensetracker;
import java.sql.Connection;
import java.sql.DriverManager;

public class database {
    public static Connection connectDb(){
        try{
            String url = "jdbc:mysql://localhost:3306/expensesdb";
            Connection connect = DriverManager.getConnection(url,"root","Vlss@1264");
            return connect;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}

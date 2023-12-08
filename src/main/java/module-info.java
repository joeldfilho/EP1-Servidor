module com.example.ep1servidor {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.ep1servidor to javafx.fxml;
    exports com.example.ep1servidor;
}
module org.example.sociable_number {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.sociable_number to javafx.fxml;
    exports org.example.sociable_number;
}
module org.cloud.cloudclient {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.cloud.cloudclient to javafx.fxml;
    exports org.cloud.cloudclient;
}
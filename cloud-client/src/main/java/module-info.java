module org.cloud.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires static lombok;
    requires cloud.common;

    opens org.cloud.client to javafx.fxml;
    exports org.cloud.client;
}
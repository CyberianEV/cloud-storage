module cloud.common {
    requires org.slf4j;
    requires static lombok;
    requires io.netty.transport;
    requires io.netty.codec;

    exports org.cloud.common;
    exports org.cloud.model;
}
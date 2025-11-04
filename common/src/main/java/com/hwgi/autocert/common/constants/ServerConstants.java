package com.hwgi.autocert.common.constants;

/**
 * 서버 관련 상수
 */
public final class ServerConstants {

    private ServerConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // 서버 타입
    public static final String TYPE_NGINX = "NGINX";
    public static final String TYPE_APACHE = "APACHE";
    public static final String TYPE_TOMCAT = "TOMCAT";
    public static final String TYPE_IIS = "IIS";
    public static final String TYPE_LIGHTTPD = "LIGHTTPD";
    public static final String TYPE_CADDY = "CADDY";

    // 서버 상태
    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_UNKNOWN = "UNKNOWN";
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";

    // 프로토콜
    public static final String PROTOCOL_SSH = "SSH";
    public static final String PROTOCOL_SFTP = "SFTP";
    public static final String PROTOCOL_SCP = "SCP";
    public static final String PROTOCOL_FTP = "FTP";

    // 기본 포트
    public static final int DEFAULT_SSH_PORT = 22;
    public static final int DEFAULT_HTTP_PORT = 80;
    public static final int DEFAULT_HTTPS_PORT = 443;
    public static final int DEFAULT_FTP_PORT = 21;

    // SSH 연결 설정
    public static final int SSH_CONNECT_TIMEOUT = 30000; // 30초
    public static final int SSH_SESSION_TIMEOUT = 60000; // 60초
    public static final int SSH_MAX_RETRIES = 3;

    // 서버 디렉토리 (Linux 기본 경로)
    public static final String NGINX_CERT_DIR = "/etc/nginx/ssl/";
    public static final String NGINX_CONFIG_DIR = "/etc/nginx/conf.d/";
    public static final String APACHE_CERT_DIR = "/etc/apache2/ssl/";
    public static final String APACHE_CONFIG_DIR = "/etc/apache2/sites-available/";
    public static final String TOMCAT_CERT_DIR = "/opt/tomcat/ssl/";
    public static final String TOMCAT_CONFIG_DIR = "/opt/tomcat/conf/";

    // 설정 파일 백업
    public static final String BACKUP_SUFFIX = ".backup";
    public static final String BACKUP_DIR = "/var/backups/autocert/";

    // 헬스체크 경로
    public static final String HEALTH_CHECK_PATH = "/health";
    public static final String HEALTH_CHECK_TIMEOUT = "5000"; // 5초

    // 재시작 명령어
    public static final String NGINX_RELOAD_CMD = "nginx -s reload";
    public static final String NGINX_TEST_CMD = "nginx -t";
    public static final String APACHE_RELOAD_CMD = "apachectl graceful";
    public static final String APACHE_TEST_CMD = "apachectl configtest";
    public static final String TOMCAT_RELOAD_CMD = "systemctl reload tomcat";
    public static final String SYSTEMCTL_RELOAD_CMD = "systemctl reload";
}
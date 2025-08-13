FROM tomcat:9.0-jdk17
ARG WAR_FILE=build/libs/*.war
COPY ${WAR_FILE} /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080

# Create entrypoint script to copy mounted config to classpath
RUN echo '#!/bin/bash' > /entrypoint.sh && \
    echo 'echo "Starting Tomcat with config setup..."' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Wait for Tomcat to extract WAR file' >> /entrypoint.sh && \
    echo 'echo "Starting Tomcat to extract WAR..."' >> /entrypoint.sh && \
    echo 'catalina.sh start' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Wait for extraction' >> /entrypoint.sh && \
    echo 'sleep 10' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Copy application.properties to classpath if mounted file exists' >> /entrypoint.sh && \
    echo 'if [ -f /config/application.properties ]; then' >> /entrypoint.sh && \
    echo '  echo "Copying application.properties to classpath..."' >> /entrypoint.sh && \
    echo '  cp /config/application.properties /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/application.properties' >> /entrypoint.sh && \
    echo '  echo "✅ application.properties copied successfully"' >> /entrypoint.sh && \
    echo 'else' >> /entrypoint.sh && \
    echo '  echo "⚠️  /config/application.properties not found"' >> /entrypoint.sh && \
    echo 'fi' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Stop Tomcat and restart normally' >> /entrypoint.sh && \
    echo 'catalina.sh stop' >> /entrypoint.sh && \
    echo 'sleep 5' >> /entrypoint.sh && \
    echo 'echo "Starting Tomcat normally..."' >> /entrypoint.sh && \
    echo 'exec catalina.sh run' >> /entrypoint.sh && \
    chmod +x /entrypoint.sh

CMD ["/entrypoint.sh"]
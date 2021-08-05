package io.github.marcodiri.java_socketio_chatroom_server;

import io.socket.engineio.server.EngineIoServer;
import io.socket.engineio.server.EngineIoServerOptions;
import io.socket.engineio.server.JettyWebSocketHandler;
import io.socket.socketio.server.SocketIoServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class ServerWrapper {

    private final Server mServer;
    private final EngineIoServer mEngineIoServer;
    private final SocketIoServer mSocketIoServer;
    
	private static final Logger LOGGER = LogManager.getLogger(ServerWrapper.class);

    @SuppressWarnings("serial")
	ServerWrapper() {

        mServer = new Server(3000);
        mEngineIoServer = new EngineIoServer(EngineIoServerOptions.newFromDefault().setPingTimeout(30000));
        mSocketIoServer = new SocketIoServer(mEngineIoServer);

        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");

        servletContextHandler.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                mEngineIoServer.handleRequest(new HttpServletRequestWrapper(request), response);
            }
        }), "/socket.io/*");

        try {
            WebSocketUpgradeFilter webSocketUpgradeFilter = WebSocketUpgradeFilter.configure(servletContextHandler);
            webSocketUpgradeFilter.addMapping(
                    new ServletPathSpec("/socket.io/*"),
                    (servletUpgradeRequest, servletUpgradeResponse) -> new JettyWebSocketHandler(mEngineIoServer));
        } catch (ServletException ex) {
        	LOGGER.warn("WebSocket is not available");
        }

        HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[]{servletContextHandler});
        mServer.setHandler(handlerList);
    }

    SocketIoServer getSocketIoServer() {
        return mSocketIoServer;
    }

    void startServer() throws Exception {
        mServer.start();
    }

    void stopServer() throws Exception {
        mServer.stop();
    }
}

package io.github.marcodiri.java_socketio_chatroom_server;

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
import java.util.concurrent.atomic.AtomicInteger;

import io.socket.engineio.server.EngineIoServer;
import io.socket.engineio.server.JettyWebSocketHandler;
import io.socket.socketio.server.SocketIoServer;


public class ServerWrapper {

    private static final AtomicInteger PORT_START = new AtomicInteger(3000);

    private final Server mServer;
    private final EngineIoServer mEngineIoServer;
    private final SocketIoServer mSocketIoServer;

    public ServerWrapper() {

        mServer = new Server(PORT_START.getAndIncrement());
        mEngineIoServer = new EngineIoServer();
        mSocketIoServer = new SocketIoServer(mEngineIoServer);

        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");

        servletContextHandler.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                mEngineIoServer.handleRequest(new HttpServletRequestWrapper(request) {
                    @Override
                    public boolean isAsyncSupported() {
                        return false;
                    }
                }, response);
            }
        }), "/socket.io/*");

        try {
            WebSocketUpgradeFilter webSocketUpgradeFilter = WebSocketUpgradeFilter.configure(servletContextHandler);
            webSocketUpgradeFilter.addMapping(
                    new ServletPathSpec("/socket.io/*"),
                    (servletUpgradeRequest, servletUpgradeResponse) -> new JettyWebSocketHandler(mEngineIoServer));
        } catch (ServletException ex) {
            // TODO add log here
        }

        HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[]{servletContextHandler});
        mServer.setHandler(handlerList);
    }

    public String getStatus() {
        return mServer.getState();
    }

    public SocketIoServer getSocketIoServer() {
        return mSocketIoServer;
    }

    public void startServer() throws Exception {
        mServer.start();
    }

    public void stopServer() throws Exception {
        mServer.stop();
    }
}

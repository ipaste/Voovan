package org.voovan.network;

import org.voovan.network.handler.SynchronousHandler;
import org.voovan.network.messagesplitter.TransferSplitter;
import org.voovan.tools.Chain;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketOption;

/**
 * socket 上下文
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class SocketContext {
	protected String host;
	protected int port;
	protected int readTimeout;

	protected IoHandler handler;
	protected Chain<IoFilter> filterChain;
	protected MessageSplitter messageSplitter;
	protected SSLManager sslManager;
	protected ConnectModel connectModel;
	protected int bufferSize = 1024;

	protected int idleInterval = 0;

	/**
	 * 构造函数
	 * 		默认不会出发空闲事件
	 * @param host    主机地址
	 * @param port    主机端口
	 * @param readTimeout 超时时间
	 */
	public SocketContext(String host,int port,int readTimeout) {
		init(host, port, readTimeout, this.idleInterval);
	}

	/**
	 * 构造函数
	 * @param host    主机地址
	 * @param port    主机端口
	 * @param idleInterval   空闲事件触发时间, 单位: 秒
	 * @param readTimeout 超时时间
	 */
	public SocketContext(String host,int port,int readTimeout, int idleInterval) {
		init(host, port, readTimeout, idleInterval);
	}

	private void init(String host,int port,int readTimeout, int idleInterval){
		this.host = host;
		this.port = port;
		this.readTimeout = readTimeout;
		this.idleInterval = idleInterval;
		connectModel = null;
		filterChain = new Chain<IoFilter>();
		this.messageSplitter = new TransferSplitter();
		this.handler = new SynchronousHandler();
	}

	protected void initSSL(IoSession session) throws SSLException {
		if (sslManager != null && connectModel == ConnectModel.SERVER) {
			sslManager.createServerSSLParser(session);
		} else if (sslManager != null && connectModel == ConnectModel.CLIENT) {
			sslManager.createClientSSLParser(session);
		}
	}

	/**
	 * 克隆对象
	 * @param parentSocketContext 父 socket 对象
	 */
	protected void copyFrom(SocketContext parentSocketContext){
		this.readTimeout = parentSocketContext.readTimeout;
		this.handler = parentSocketContext.handler;
		this.filterChain = parentSocketContext.filterChain;
		this.messageSplitter = parentSocketContext.messageSplitter;
		this.sslManager = parentSocketContext.sslManager;
		this.bufferSize = parentSocketContext.bufferSize;
		this.idleInterval = parentSocketContext.idleInterval;
	}

	/**
	 * 获取空闲事件时间
	 * @return  空闲事件时间, 单位:秒
	 */
	public int getIdleInterval() {
		return idleInterval;
	}

	/**
	 * 设置空闲事件时间
	 * @param idleInterval  空闲事件时间, 单位:秒
	 */
	public abstract void setIdleInterval(int idleInterval);

	/**
	 * 设置 Socket 的 Option 选项
	 *
	 * @param name   SocketOption类型的枚举, 参照:AsynchronousSocketChannel.setOption的说明
	 * @param value  SocketOption参数
	 * @param <T> 范型
	 * @throws IOException IO异常
	 */
	public abstract  <T> void setOption(SocketOption<T> name, T value) throws IOException;

	/**
	 * 获取缓冲区大小
	 * @return 缓冲区大小 (default:1024)
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * 设置缓冲区大小
	 * @param bufferSize 缓冲区大小 (default:1024)
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * 无参数构造函数
	 */
	protected SocketContext() {
		filterChain = new Chain<IoFilter>();
	}

	/**
	 * 获取 SSL 管理器
	 * @return SSL 管理器
	 */
	public SSLManager getSSLManager() {
		return sslManager;
	}

	/**
	 * 设置 SSL 管理器
	 * @param sslManager SSL 管理器
	 */
	public void setSSLManager(SSLManager sslManager) {
		if(this.sslManager==null){
			this.sslManager = sslManager;
		}
	}

	/**
	 * 获取主机地址
	 * @return 主机地址
	 */
	public String getHost() {
		return host;
	}

	/**
	 * 获取主机端口
	 * @return 主机端口
	 */
	public int getPort() {
		return port;
	}

	/**
	 * 获取超时时间
	 * @return 超时时间
	 */
	public int getReadTimeout() {
		return readTimeout;
	}

	/**
	 * 获取连接模式
	 * @return 连接模式
	 */
	public ConnectModel getConnectModel() {
		return connectModel;
	}

	/**
	 * 获取业务处理句柄
	 * @return 业务处理句柄
	 */
	public IoHandler handler(){
		return this.handler;
	}

	/**
	 * 设置业务处理句柄
	 * @param handler 业务处理句柄
	 */
	public void handler(IoHandler handler){
		this.handler = handler;
	}

	/**
	 * 获取过滤器链
	 * @return 过滤器链
	 */
	public Chain<IoFilter> filterChain(){
		return this.filterChain;
	}

	/**
	 * 获取消息粘包分割器
	 * @return 消息粘包分割器
	 */
	public MessageSplitter messageSplitter() {
		return this.messageSplitter;
	}

	/**
	 * 设置消息粘包分割器
	 * @param  messageSplitter 消息分割器
	 */
	public void messageSplitter(MessageSplitter messageSplitter) {
		this.messageSplitter = messageSplitter;
	}

	/**
	 * 启动上下文连接
	 *		阻塞方法
	 * @throws IOException IO 异常
	 */
	public abstract void start() throws IOException;

	/**
	 * 启动同步的上下文连接
	 * 		非阻塞方法
	 *
	 * @exception IOException IO异常
	 */
	public abstract void syncStart() throws IOException;

	/**
	 * 用于针对 Accept 进来的 Socket 连接的启动
	 * @throws IOException IO异常
	 */
	protected abstract void acceptStart() throws IOException;

	/**
	 * 等待连接完成
	 * @param session socket 会话对象
	 */
	protected void waitConnected(IoSession session){
		try {
			int waitConnectTime = 0;
			while (!isConnected()) {
				TEnv.sleep(1);
				waitConnectTime++;
				if (waitConnectTime >= readTimeout) {
					break;
				}
			}

			//等待 SSL 握手操作完成
			while (session.getSSLParser() != null &&
					!session.getSSLParser().isHandShakeDone() &&
					isConnected()) {
				TEnv.sleep(1);
			}
		}catch(Exception e){
			Logger.error(e);
			session.close();
		}
	}

	/**
	 * 上下文连接是否打开
	 * @return true:连接打开,false:连接关闭
	 */
	public abstract boolean isOpen();


	/**
	 * 上下文连接是否连接
	 * @return true:连接,false:断开
	 */
	public abstract boolean isConnected();

	/**
	 * 关闭连接
	 * @return 是否关闭
	 */
	public abstract boolean close();
}

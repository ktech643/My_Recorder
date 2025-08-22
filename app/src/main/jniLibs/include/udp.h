#ifndef UDP_H_DEF
#define UDP_H_DEF

#include <stdio.h>
#include <stdlib.h>
#include <Ws2TcpIp.h>
#include <windows.h>
#include <time.h>
#pragma comment(lib, "WS2_32.lib")

#define BUFLEN 512



struct UdpPacket {
	double time = 0;
};

struct HelloPack {		//start key. add what you want
	UINT64 Key = 88888888888888;
};

struct ThreadDesc {
	DWORD ThreadID = 0;
	HANDLE handle = NULL;
	bool bRun = false;
};


struct Client
{
	Client(sockaddr_in _si, int sout, HelloPack* _pack, bool nanswer = true) :
		client_sockaddr(_si),
		needanswer(nanswer),
		socketout(sout),
		startPack(*_pack)
	{
		next = NULL;
	}
	HelloPack startPack;
	bool needanswer;
	sockaddr_in client_sockaddr;
	Client *next;
	int socketout;
};

class UdpServer {
public:
	UdpServer(UINT16 _serverPort) : serverPort(_serverPort) {
		InitializeCriticalSection(&clientMut);
	};
	
	~UdpServer() {
		StopServer();
	};

	void die(const char *s, int _sock = 0)
	{
		if (_sock)
			closesocket(_sock);
		WSACleanup();
	}

	DWORD WINAPI listner_thread_func()
	{
		sockaddr_in si_me;
		HelloPack* hello = NULL;
		int listnerSock = INVALID_SOCKET;
		int i, recv_len;
		int slen = sizeof(sockaddr_in);
		char buf[BUFLEN];
		listnerSock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
		if (listnerSock == INVALID_SOCKET) {
			die("socket", listnerSock);
		}

		memset((char *)&si_me, 0, sizeof(si_me));
		si_me.sin_family = AF_INET;
		si_me.sin_port = htons(serverPort);
		si_me.sin_addr.s_addr = htonl(INADDR_ANY);

		if (bind(listnerSock, (struct sockaddr*)&si_me, sizeof(si_me)) == -1) {
			die("socket", listnerSock);
		}

		while (1) {
			printf("Waiting for data...\n");
			fflush(stdout);

			sockaddr_in si_other;
			if ((recv_len = recvfrom(listnerSock, buf, BUFLEN, 0, (struct sockaddr *) &si_other, &slen)) == -1) {
				die("socket", listnerSock);
			}
			if (recv_len != sizeof(HelloPack))
			{
				//неверный начальный пакет
				continue;
			}
			hello = (HelloPack*)buf;
			if(hello->Key != HelloPack().Key)
			Lock();
			if (clients == NULL) {
				clients = new Client(si_other, listnerSock, (HelloPack*)buf);
			}
			else {
				Client *cur = clients;
				while (cur->next != NULL) {
					cur = cur->next;
				}
				cur->next = new Client(si_other, listnerSock, (HelloPack*)buf);;
			}
			Unlock();
		}
		listner.bRun = false;
		closesocket(listnerSock);
		WSACleanup();
		return NULL;
	}

	DWORD WINAPI sender_thread_func() {
		char bout[512];
		size_t slen = sizeof(sockaddr_in);
		sender.bRun = true;
		while (sender.bRun) {
			Lock();
			Client *cur = clients;
			while (cur != NULL) {
				bout[0] = 0;
				int snd = sendto(cur->socketout, (char*)&curentPack, sizeof(curentPack), 0, (struct sockaddr*) &cur->client_sockaddr, slen);
				if (snd == -1) {
					//что-то пошло не так =)
				}
				cur = cur->next;
			}
			Unlock();
			Sleep(50);		//todo установить необходимую частоту обновлений
		}
		sender.bRun = false;
		return NULL;
	}

	static DWORD WINAPI udp_server_listner(void* server) {
		return ((UdpServer*)server)->listner_thread_func();
	}
	static DWORD WINAPI udp_server_sender(void* server) {
		return ((UdpServer*)server)->sender_thread_func();
	}
	
	void SetCurentPack(UdpPacket _pack) {	//вызываем извне
		Lock();
		curentPack = _pack;
		Unlock();
	}
	
	void Lock() {
		EnterCriticalSection(&clientMut);
	}
	
	void Unlock() {
		LeaveCriticalSection(&clientMut);
	}
	
	bool StartServer() {
		if (WSAStartup(0x0101, &w) != 0)
		{
			return false;
		}
		listner.handle = CreateThread(NULL, 0, UdpServer::udp_server_listner, this, 0, &listner.ThreadID);
		sender.handle = CreateThread(NULL, 0, UdpServer::udp_server_sender, this, 0, &sender.ThreadID);
		return listner.handle == NULL && sender.handle == NULL;
	}

	bool StopServer() {
		sender.bRun = false;
		listner.bRun = false;
		if (listner.handle && WaitForSingleObject(listner.handle, 1000) == WAIT_OBJECT_0)
			listner.handle = NULL;
		if (sender.handle && WaitForSingleObject(sender.handle, 1000) == WAIT_OBJECT_0)
			sender.handle = NULL;
		return listner.handle && sender.handle;
	}
	
	
private :
	WSADATA w;
	CRITICAL_SECTION clientMut;
	CRITICAL_SECTION udpPacketMut;
	const char *hellostring = "client v1.0";
	
	UINT16 serverPort;

	Client *clients = NULL;

	UdpPacket curentPack;	//передаваемый пакет

	ThreadDesc sender;		
	ThreadDesc listner;
};




class UdpClient {
public:
	UdpClient(UINT16 _port,const char* _serverIp) {
		InitializeCriticalSection(&curentMut);
		memset((char *)&serverInfo, 0, sizeof(serverInfo));
		serverInfo.sin_family = AF_INET;
		serverInfo.sin_port = htons(_port);
		inet_pton(AF_INET, _serverIp, &serverInfo.sin_addr);
	}
	void die(const char *s, int _sock = 0)
	{
		if (_sock)
			closesocket(_sock);
		WSACleanup();
	}

	DWORD WINAPI listner_thread_func()
	{
		listner.bRun = true;
		Client *client = NULL;
		UdpPacket* pack = NULL;
		int listnerSock, recv_len;
		int slen = sizeof(sockaddr_in);
		char buf[BUFLEN];

		if ((listnerSock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == INVALID_SOCKET) {
			die("socket", -1);
		}

		if (sendto(listnerSock, (char*)&startPack, sizeof(HelloPack), 0, (struct sockaddr *) &serverInfo, slen) == -1) {
			die("sendto()", listnerSock);
		}

		while (1) {
			if ((recv_len = recvfrom(listnerSock, buf, BUFLEN, 0, (struct sockaddr *) &serverInfo, &slen)) == -1) {
				die("recvfrom()", listnerSock);
			}
			if (recv_len != sizeof(UdpPacket))
			{
				//неверный пакет
				continue;
			}
			pack = (UdpPacket*)buf;
			Lock();
			currentPack = *pack;
			Unlock();
		}
		listner.bRun = false;
		closesocket(listnerSock);
		WSACleanup();
		return NULL;
	}
	static DWORD WINAPI udp_client_listner(void* _client) {
		return ((UdpClient*)_client)->listner_thread_func();
	}

	void SetServerInfo(UINT16 _port, char* _serverIp) {
		memset((char *)&serverInfo, 0, sizeof(serverInfo));
		serverInfo.sin_family = AF_INET;
		serverInfo.sin_port = htons(_port);
		serverInfo.sin_addr.s_addr = inet_pton(AF_INET, _serverIp, &serverInfo.sin_addr.S_un.S_un_b);
	}

	~UdpClient() {
		if(listner.bRun)
			StopClient();
	}
	
	bool StopClient() {
		listner.bRun = false;
		if (listner.handle && WaitForSingleObject(listner.handle, 1000) != WAIT_OBJECT_0)
			listner.handle = NULL;
		return listner.handle;
	}

	UdpPacket GetCurrentPack() {	//если пакет будет большим переписать на указатели
		UdpPacket pack;
		Lock();
		pack = currentPack;
		Unlock();
		return pack;
	}
	
	bool StartClient() {
		if (WSAStartup(0x0101, &w) != 0)
		{
			return false;
		}
		listner.handle = CreateThread(NULL, 0, UdpClient::udp_client_listner, this, 0, &listner.ThreadID);
		return listner.handle != NULL;
	}
	
private:
	
	void Lock() {
		EnterCriticalSection(&curentMut);
	}
	
	void Unlock() {
		LeaveCriticalSection(&curentMut);
	}
private:
	WSADATA w;
	CRITICAL_SECTION curentMut;
	sockaddr_in serverInfo;
	HelloPack startPack;
	UdpPacket currentPack;
	ThreadDesc listner;
};


#endif
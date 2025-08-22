#ifndef TIMER_LIST
#define TIMER_LIST

struct MyTimer{
	MyTimer() {
		timer = CreateWaitableTimer(NULL, TRUE, NULL);
	}
	~MyTimer() {
		CancelWaitableTimer(timer);
	}
	HANDLE timer = NULL;
	long isUsed = 0;
};

class TimerList {
public :
	TimerList(int _count = 10) : m_timerCount(_count) 
	{ 
		m_timers = new MyTimer[_count]; 
	}
	~TimerList() {
		delete[] m_timers;
	}
	HANDLE GetTimer() {
		while (true) {
			for (int i = 0; i < m_timerCount; ++i) {
					if (!InterlockedCompareExchange(&m_timers[i].isUsed, 1, 0)) {
					m_timers[i].isUsed = true;
					return m_timers[i].timer;
				}
			}
		}
		return NULL;
	}
	void ReturnTimer(HANDLE _handler) {
		for (int i = 0; i < m_timerCount; ++i) {
			if (m_timers[i].timer == _handler) {
				m_timers[i].isUsed = 0;
				return;
			}
		}
	}

	void ReturnAll() {
		for (int i = 0; i < m_timerCount; ++i) {
			m_timers[i].isUsed = false;
		}
	}
private:
	int m_timerCount = 0;
	MyTimer* m_timers;
};
#endif
#ifndef SOURCE_LIST
#define SOURCE_LIST
#include "../VS_Abstract.h"
class SourceList {
public:
	int addItem(AbstractSource* _item) {
		if (id == 0) {
			id = 1;
			item = _item;
			return id;
		}
		SourceList* curent = this;
		while (curent->next != NULL)
			curent = curent->next;
		curent->next = (SourceList*)rdr_alloc(sizeof(SourceList));
		curent->next->prev = curent;
		curent = curent->next;
		curent->id = curent->prev->id + 1;
		curent->item = _item;
		return curent->id;
	}

	bool removeItem(int _id) {
		SourceList* curent = getItem(_id);
		if (curent == NULL)
			return false;

		if (curent->item != NULL) {
			if (curent->prev == NULL) {
				delete curent->item;
				curent->item = NULL;
				curent->id = 0;
				/*if (curent->next != NULL)
					curent->next->prev = curent->prev;
				if (curent->prev != NULL)
					curent->prev->next = curent->next;
				if(curent != this)
					delete curent;*/
				return true;
			}
			if (prev != NULL) {
				//switchToprev
			}
			else {
				//switchToNext
			}
		}
		if (curent->item != NULL) {
			delete curent->item;
			curent->item = NULL;
		}
		if (curent->next != NULL)
			curent->next->prev = curent->prev;
		if (curent->prev != NULL) {
			curent->prev->next = curent->next;
			delete curent;
		}
	}

	AbstractSource* getSource(int _id) {
		SourceList* curent = getItem(_id);
		if (curent != NULL)
			return curent->item;
		else
			return NULL;
	}

	SourceList* getItem(int _id) {
		SourceList* curent = this;
		while (curent->id != _id && curent->next != NULL)
			curent = curent->next;
		if (curent->id != _id)
			return NULL;
		return curent;
	}

	void clear() {
		SourceList* curent = this;
		SourceList* prev = NULL;
		while (curent->next != NULL)
			curent = curent->next;
		if (curent->prev == NULL)
			removeItem(curent->id);
		else {
			prev = curent->prev;
			do {
				prev = curent->prev;
				removeItem(curent->id);
				curent = prev;
			} while (prev != NULL);
		}
	}

	int id = 0;
	bool isCurent = false;
	AbstractSource* item = NULL;
	SourceList* prev = NULL;
	SourceList* next = NULL;
};
#endif //SOURCE_LIST
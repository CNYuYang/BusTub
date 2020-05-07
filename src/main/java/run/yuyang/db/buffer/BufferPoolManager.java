package run.yuyang.db.buffer;

import lombok.extern.slf4j.Slf4j;
import run.yuyang.db.recovery.LogManger;
import run.yuyang.db.storage.disk.DiskManager;
import run.yuyang.db.storage.page.Page;

import java.util.*;

import static run.yuyang.db.util.Config.PAGE_SIZE;

/**
 * @author YuYang
 */
@Slf4j
public class BufferPoolManager {

    private final DiskManager diskManager;
    private final LogManger logManger;
    private final int poolSize;
    private final ClockReplacer clockReplacer;
    private final Page[] pages;
    private final LinkedList<Integer> freeList = new LinkedList<>();
    private final Object latch = new Object();
    private final HashMap<Integer, Integer> map = new HashMap<>();

    public BufferPoolManager(DiskManager diskManager, LogManger logManger, int poolSize) {
        this.diskManager = diskManager;
        this.logManger = logManger;
        this.poolSize = poolSize;
        clockReplacer = new ClockReplacer(poolSize);
        pages = new Page[poolSize];
        for (int i = 0; i < poolSize; i++) {
            freeList.add(i);
        }
    }

    /**
     * �ӻ�����л�ȡ�����ҳ��
     *
     * @param pageId ҳ��id
     * @return ��Ӧ��page
     */
    public Page fetchPageImpl(int pageId) {

        if (map.containsKey(pageId)) {
            clockReplacer.pin(map.get(pageId));
            freeList.remove(map.get(pageId));
            return pages[map.get(pageId)];
        }
        int frameId;
        if (!freeList.isEmpty()) {
            frameId = freeList.pollFirst();
        } else {
            frameId = clockReplacer.victim();

            if (frameId == -1) {
                return null;
            }
            if (pages[frameId].isDirty()) {
                flushPageImpl(pages[frameId].getPageId());
            }
            map.remove(pageId);
        }
        map.put(pageId, frameId);
        Page page = new Page();
        page.setPageId(pageId);
        page.setPinCount(1);
        page.setDirty(false);
        byte[] data = new byte[PAGE_SIZE];
        diskManager.readPage(pageId, data);
        page.setData(data);
        pages[frameId] = page;
        return page;
    }


    /**
     * ȡ��ָ��ҳ��page�Ĺ̶�
     *
     * @param pageId  page_idҪȡ���̶���ҳ���ID
     * @param isDirty is_dirty���ҳ��Ӧ���Ϊ�࣬��Ϊtrue������Ϊfalse
     * @return ����ڴ˵���֮ǰҳ��̶�����<= 0 �� �򷵻�false �� ���򷵻�true
     */
    public boolean unpinPageImpl(int pageId, boolean isDirty) {

        if (map.containsKey(pageId)) {

            if (pages[map.get(pageId)].getPinCount() <= 0) {
                return false;
            }
            pages[map.get(pageId)].setDirty(pages[map.get(pageId)].isDirty() || isDirty);
            pages[map.get(pageId)].setPinCount(pages[map.get(pageId)].getPinCount() - 1);
            if (pages[map.get(pageId)].getPinCount() == 0) {
                clockReplacer.unpin(map.get(pageId));
            }
            return true;
        }
        return false;
    }

    /**
     * ��Ŀ��ҳ��ˢ�µ����̡�
     *
     * @param pageId page_idҪˢ�µ�ҳ���ID������ΪINVALID_PAGE_ID
     * @return �����ҳ������Ҳ�����ҳ�棬�򷵻�false�����򷵻�true
     */
    public boolean flushPageImpl(int pageId) {
        if (map.containsKey(pageId) && pages[map.get(pageId)].isDirty()) {
            pages[map.get(pageId)].setDirty(false);
            diskManager.writePage(pageId, pages[map.get(pageId)].getData());
            return true;
        }
        return false;
    }

    /**
     * ��buffer pool�д���һ���µ�page
     *
     * @return �µ�page
     */
    public Page newPageImpl() {

        int frameId;
        if (!freeList.isEmpty()) {
            frameId = freeList.pollFirst();

        } else {
            frameId = clockReplacer.victim();
            if (frameId == -1) {
                return null;
            }
            if (pages[frameId].isDirty()) {
                flushPageImpl(pages[frameId].getPageId());
            }
            map.remove(pages[frameId].getPageId());
        }
        int pageId = diskManager.allocatePage();
        map.put(pageId, frameId);
        Page page = new Page();
        page.setPageId(pageId);
        page.setPinCount(1);
        page.setDirty(false);
        pages[frameId] = page;
        return page;
    }

    /**
     * �ӻ������ɾ��ҳ��
     *
     * @param pageId page_idҪɾ����ҳ���ID
     * @return �����ҳ����ڵ��޷�ɾ�����򷵻�false�������ҳ�治���ڻ�ɾ���ɹ����򷵻�true
     */
    public boolean deletePageImpl(int pageId) {
        diskManager.deallocatePage(pageId);
        if (map.containsKey(pageId)) {
            if (pages[map.get(pageId)].getPinCount() > 0) {
                return false;
            }
            pages[pageId] = null;
            freeList.add(map.get(pageId));
            map.remove(pageId);
        }

        return true;
    }

    /**
     * ��������е�����ҳ��ˢ�µ����̡�
     */
    public void flushAllPagesImpl() {
        for (Integer integer : map.keySet()) {
            flushPageImpl(integer);
        }
    }
}

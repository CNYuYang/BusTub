package run.yuyang.db.container;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import run.yuyang.db.buffer.BufferPoolManager;
import run.yuyang.db.storage.disk.DiskManager;
import run.yuyang.db.storage.page.IntegerBlockPage;
import run.yuyang.db.storage.page.HashTableHeaderPage;
import run.yuyang.db.storage.page.Page;

import java.io.File;

@Slf4j
public class HashTablePageTest extends TestCase {

    @Test
    public void test1() {

        DiskManager diskManager = new DiskManager("test.db");
        BufferPoolManager poolManager = new BufferPoolManager(diskManager, null, 5);
        Page page0 = poolManager.newPageImpl();
        HashTableHeaderPage headerPage = new HashTableHeaderPage();
        page0.setData(headerPage);

        // ����һЩ�ֶ�
        for (int i = 0; i < 11; i++) {
            headerPage.setLsn(i);
            assertEquals(headerPage.getLsn(), i);
            headerPage.setPageId(i);
            assertEquals(headerPage.getPageId(), i);
            headerPage.setSize(i);
            assertEquals(headerPage.getSize(), i);
        }

        //���һЩ�����block pages
        for (int i = 0; i < 10; i++) {
            headerPage.addBlockPageIds(i);
            assertEquals(i + 1, headerPage.numBlocks());
        }
        // ʹ�ý���
        log.debug("id: {}", page0.getPageId());
        log.debug("data : {}", page0.getData());
        poolManager.unpinPageImpl(page0.getPageId(), true);
        File file = new File("test.db");
        file.delete();
        file = new File("test.log");
        file.delete();
    }

    @Test
    public void test2() {

        IntegerBlockPage blockPage = new IntegerBlockPage();

        //����һЩkey value��
        for (int i = 9; i >=0 ; i--) {
            blockPage.insert(i, i, i);
        }

        //�������key value��
        for (int i = 0; i < 10; i++) {
            assertEquals(i, blockPage.keyAt(i).intValue());
            assertEquals(i, blockPage.valueAt(i).intValue());
        }

        //�Ƴ�һЩkey value��
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 1) {
                blockPage.remove(i);
            }
        }

        //���flag
        for (int i = 0; i < 15; i++) {
            if (i < 10) {
                assertTrue(blockPage.isOccupied(i));
                if (i % 2 == 1) {
                    assertFalse(blockPage.isReadable(i));
                } else {
                    assertTrue(blockPage.isReadable(i));
                }
            } else {
                assertFalse(blockPage.isOccupied(i));
            }
        }
    }

}

package run.yuyang.db.storage.page;

public interface Pageable {

    /**
     * ת������byte�����С��ΪPAGE_SIZE
     */
    byte[] convertTo();

    void convertBack(byte[] data);


}

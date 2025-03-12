package sms.admin.util.exporter;

import java.util.List;

public interface TableDataProvider<T> {
    String getSheetName();

    List<String> getHeaders();

    List<String> getRowData(T item);
}

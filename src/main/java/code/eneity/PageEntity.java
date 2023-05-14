package code.eneity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PageEntity<T> {

    private int total;
    private int remainder;
    private int page;
    private int count;
    private int current;
    private boolean hasNext;
    private boolean hasPrev;
    private List<T> list = new ArrayList<>();

    private PageEntity() {}

    public PageEntity(int total, int page, int current) {
        this.total = total;
        this.remainder = total % page;
        this.page = page;
        this.count = remainder > 0 ? ((total / page) + 1) : (total / page);
        this.current = current;

        this.hasNext = current < this.count;
        this.hasPrev = current > 1;
    }

    public static PageEntity empty() {
        return new PageEntity();
    }

}

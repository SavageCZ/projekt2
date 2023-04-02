package cz.mendelu.ja.projekt2;

import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class TraceResult {
    public final List<String> countries;
    public final AtomicInteger hopCount;
}

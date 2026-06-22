package com.college.placement.shared.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class FilePipelineMetrics {

    private final Counter uploadCounter;
    private final Counter scanCleanCounter;
    private final Counter scanInfectedCounter;
    private final Counter scanFailedCounter;
    private final Timer scanDurationTimer;
    private final MeterRegistry registry;

    public FilePipelineMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.uploadCounter = Counter.builder("file.upload.total")
                .description("Total files uploaded")
                .register(registry);
        this.scanCleanCounter = Counter.builder("file.scan.clean.total")
                .description("Total files scanned clean")
                .register(registry);
        this.scanInfectedCounter = Counter.builder("file.scan.infected.total")
                .description("Total files detected as infected")
                .register(registry);
        this.scanFailedCounter = Counter.builder("file.scan.failed.total")
                .description("Total files where scanning failed")
                .register(registry);
        this.scanDurationTimer = Timer.builder("file.scan.duration")
                .description("File virus scan processing time")
                .register(registry);
    }

    public void recordUpload()        { uploadCounter.increment(); }
    public void recordScanClean()     { scanCleanCounter.increment(); }
    public void recordScanInfected()  { scanInfectedCounter.increment(); }
    public void recordScanFailed()    { scanFailedCounter.increment(); }

    public Timer.Sample startSample()                  { return Timer.start(registry); }
    public void stopScanTimer(Timer.Sample sample)     { sample.stop(scanDurationTimer); }
}

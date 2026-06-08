package com.klogviewer.core.parser

object StructuredEcosystemFixtures {
    const val LOGSTASH_LOGBACK_JSON =
        """{"@timestamp":"2026-06-01T10:00:00Z","level":"INFO","message":"logstash handled request","logger_name":"com.example.Orders","mdc":{"traceId":"trace-jvm-1","spanId":"span-jvm-1","correlationId":"corr-jvm-1"},"payload":{"orderId":"o-123"}}"""

    const val LOGBACK_JSON_WITH_MDC =
        """{"timestamp":"2026-06-01T10:00:01Z","level":"WARN","message":"logback warning","logger":"com.example.Inventory","traceId":"trace-jvm-2","spanId":"span-jvm-2","MDC":{"tenant":"blue","requestId":"req-jvm-2"}}"""

    const val SPRING_BOOT_STRUCTURED_JSON =
        """{"timestamp":"2026-06-01T10:00:02Z","level":"ERROR","logger":"org.springframework.web","message":"spring failed request","exception":"IllegalStateException","traceId":"trace-spring-1","spanId":"span-spring-1","context":{"service":"orders"}}"""

    const val LOG4J2_JSON_LAYOUT =
        """{"timeMillis":1780308003000,"level":"DEBUG","loggerName":"org.example.log4j.Layout","message":"layout event","thrown":"java.lang.RuntimeException: boom","trace_id":"trace-log4j-1","span_id":"span-log4j-1"}"""

    const val LOG4J2_JSON_TEMPLATE_LAYOUT =
        """{"@timestamp":"2026-06-01T10:00:04Z","Level":"INFO","LoggerName":"org.example.log4j.Template","Message":"template layout event","Exception":"java.lang.IllegalArgumentException","trace.id":"trace-log4j-2","span.id":"span-log4j-2"}"""

    const val MEL_JSON_CONSOLE =
        """{"Timestamp":"2026-06-01T10:01:00Z","LogLevel":"Information","Category":"Microsoft.Hosting.Lifetime","EventId":{"Id":42,"Name":"Startup"},"Message":"MEL application started","Exception":null,"Scopes":[{"RequestId":"req-mel-1"}],"TraceId":"trace-mel-1","SpanId":"span-mel-1","RequestId":"req-mel-1"}"""

    const val SERILOG_COMPACT_JSON =
        """{"@t":"2026-06-01T10:01:01Z","@mt":"Order {OrderId} failed for {UserId}","@l":"Error","@x":"System.Exception: boom","@i":"evt-001","@tr":"trace-seri-1","@sp":"span-seri-1","Properties":{"OrderId":"o-001","UserId":"u-123","SourceContext":"OrdersController","RequestId":"req-seri-1"}}"""

    const val SERILOG_RENDERED_COMPACT_JSON =
        """{"@t":"2026-06-01T10:01:02Z","@mt":"HTTP {RequestMethod} {RequestPath} responded {StatusCode} in {Elapsed:0.0000} ms","@m":"HTTP GET /orders responded 500 in 123.45 ms","@l":"Error","@tr":"trace-seri-2","@sp":"span-seri-2","RequestPath":"/orders","RequestMethod":"GET","StatusCode":500,"Elapsed":123.45,"RequestId":"req-seri-2"}"""

    const val SERILOG_STANDARD_JSON =
        """{"Timestamp":"2026-06-01T10:01:03Z","Level":"Warning","MessageTemplate":"Queue {QueueName} delayed","RenderedMessage":"Queue payments delayed","Exception":null,"Properties":{"QueueName":"payments","SourceContext":"QueueWorker","RequestId":"req-seri-3","TraceId":"trace-seri-3","SpanId":"span-seri-3"}}"""

    const val SERILOG_ASPNET_REQUEST_JSON =
        """{"@t":"2026-06-01T10:01:04Z","@mt":"HTTP {RequestMethod} {RequestPath} responded {StatusCode} in {Elapsed:0.0000} ms","@m":"HTTP POST /checkout responded 201 in 55.20 ms","@l":"Information","RequestPath":"/checkout","RequestMethod":"POST","StatusCode":201,"Elapsed":55.2,"RequestId":"req-aspnet-1","TraceId":"trace-aspnet-1","SpanId":"span-aspnet-1"}"""

    const val NLOG_JSON_LAYOUT =
        """{"time":"2026-06-01T10:01:05Z","level":"Warn","logger":"NLogLogger","message":"nlog warning","exception":"System.InvalidOperationException","properties":{"tenant":"green","requestId":"req-nlog-1","traceId":"trace-nlog-1","spanId":"span-nlog-1"}}"""

    const val LOG4NET_JSON_STYLE =
        """{"timestamp":"2026-06-01T10:01:06Z","level":"ERROR","logger":"Log4Net.App","message":"log4net failed","exception":"System.Exception","properties":{"correlationId":"corr-log4net-1","requestId":"req-log4net-1"}}"""

    const val DOCKER_JSON_WRAPPER =
        """{"log":"{\"timestamp\":\"2026-06-01T11:00:00Z\",\"level\":\"INFO\",\"message\":\"docker app ready\",\"logger\":\"com.example.Docker\",\"traceId\":\"trace-docker-1\",\"spanId\":\"span-docker-1\",\"RequestId\":\"req-docker-1\"}\n","stream":"stdout","time":"2026-06-01T11:00:00.123456789Z"}"""

    const val KUBERNETES_CRI_WRAPPER =
        """{"time":"2026-06-01T11:00:01.123456789Z","stream":"stderr","kubernetes":{"namespace":"payments","pod":"checkout-7f89"},"log":"{\"timestamp\":\"2026-06-01T11:00:01Z\",\"level\":\"ERROR\",\"message\":\"k8s app failed\",\"logger\":\"com.example.K8s\",\"traceId\":\"trace-k8s-1\",\"spanId\":\"span-k8s-1\",\"requestId\":\"req-k8s-1\"}"}"""

    const val CLOUD_PROVIDER_ENVELOPE =
        """{"insertId":"cloud-1","severity":"NOTICE","resource":{"type":"cloud_run_revision","labels":{"service_name":"billing"}},"jsonPayload":{"timestamp":"2026-06-01T11:00:02Z","level":"WARN","message":"cloud nested app warning","logger":"com.example.Cloud","traceId":"trace-cloud-1","spanId":"span-cloud-1","RequestId":"req-cloud-1"}}"""

    const val OTEL_LIKE_JSON =
        """{"timeUnixNano":"1780308003000123456","severityText":"ERROR","body":"otel export failed","resource":{"service.name":"checkout","service.namespace":"payments"},"attributes":{"http.method":"POST","http.route":"/checkout","RequestId":"req-otel-1","trace_id":"trace-otel-1","span_id":"span-otel-1"}}"""
}

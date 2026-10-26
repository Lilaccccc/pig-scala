package pig
package utils.time

import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

def monthByLocalDateTime(time: LocalDateTime): Array[LocalDateTime] = Array(
  time
    .`with`(TemporalAdjusters.firstDayOfMonth())
    .withHour(0)
    .withMinute(0)
    .withSecond(0)
    .withNano(0), // 月初 00:00:00
  time
    .`with`(TemporalAdjusters.lastDayOfMonth())
    .withHour(23)
    .withMinute(59)
    .withSecond(59)
    .withNano(0) // 月末 23:59:59
)

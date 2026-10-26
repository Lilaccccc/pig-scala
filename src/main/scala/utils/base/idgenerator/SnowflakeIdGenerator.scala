package pig
package utils.base.idgenerator

class SnowflakeIdGenerator private (
  val workerId: Long, // 机器ID
  val datacenterId: Long // 数据中心ID
) {
  import SnowflakeIdGenerator.*

  require(workerId >= 0 && workerId < MaxWorkerId, s"workerId must be in [0, ${MaxWorkerId - 1}].")
  require(datacenterId >= 0 && datacenterId < MaxDatacenterId, s"datacenterId must be in [0, ${MaxDatacenterId - 1}].")

  // 序列号
  private var sequence: Long = 0L
  // 上次生成 ID 的时间戳（这里记录的是“逻辑时间戳”，即补偿后的时间，保证严格单调不减）
  // 注意：lastTimestamp 是保存在内存里的，进程一重启，这个值就丢了
  // - 运行中回拨 → lastTimestamp 还在，能检测到 → 用 clockOffset 补偿 ✅
  // - 重启后回拨 → lastTimestamp = -1，检测不到 → 生成重复 ID ❌（需持久化解决）
  private var lastTimestamp: Long = -1L
  // 用于跟踪时钟回拨的时间差（补偿量）
  // 回拨时 clockOffset > 0，逻辑时间 = 真实时间 + clockOffset，从而追平回拨前的时间
  // 时钟恢复正常后，clockOffset 会被逐步“消化”或直接重置为 0
  // 和 lastTimestamp 一样，clockOffset 只在内存，重启后归零，回拨补偿信息丢失
  private var clockOffset: Long   = 0L

  // 生成唯一ID，使用 synchronized 避免线程不安全问题
  def generateUniqueId(): Long = synchronized {
    val now = currentTimeMillis()

    // 计算本次的补偿量：
    // - 如果真实时间小于 lastTimestamp（回拨了），用 lastTimestamp - now 作为补偿，
    //   使得逻辑时间 = now + clockOffset = lastTimestamp，保持单调不减
    // - 如果真实时间已经追上或超过 lastTimestamp，说明回拨已恢复，补偿归零
    if now < lastTimestamp then {
      // 回拨场景：把补偿量设成能追平 lastTimestamp 所需的最小值
      clockOffset = lastTimestamp - now
    } else {
      // 正常/已恢复场景：不再需要补偿
      clockOffset = 0L
    }

    // 逻辑时间 = 真实时间 + 补偿量
    // 用 max 再兜底一次，确保逻辑时间严格不小于 lastTimestamp，
    // 避免极端情况下（如 clockOffset 计算误差）出现逻辑时间倒退
    var timestamp = math.max(now + clockOffset, lastTimestamp)

    // 同一（逻辑）毫秒内自增序列号
    if timestamp == lastTimestamp then {
      // 自增并掩码
      sequence = (sequence + 1) & SequenceMask
      // 序列号用完，说明这一毫秒已经产出 4096 个 ID，
      // 不能再靠补偿“赖”在同一毫秒里，必须等真实时间前进到下一毫秒
      if sequence == 0L then {
        // 等到真实时间严格大于“当前逻辑时间对应的真实时间”
        // 即等到 now 超过 timestamp - clockOffset
        val nextReal = waitNextMillis(timestamp - clockOffset)
        // 用新的真实时间重新校准：逻辑时间抬到 nextReal，补偿清零
        clockOffset = 0L
        timestamp   = nextReal
      }
    } else {
      // 不同（逻辑）毫秒，序列号重置
      sequence = 0L
    }

    lastTimestamp = timestamp

    // 组合生成唯一ID（注意：这里直接用已补偿好的 timestamp，不再重复加 clockOffset）
    ((timestamp - Epoch) << TimestampShift) // 22
      | (datacenterId << DatacenterIdShift) // 17
      | (workerId << WorkerIdShift)         // 12
      | sequence                            // 0
  }

  // 等待下一毫秒，直到获得新的时间戳
  // 入参 base 是“真实时间”的基准，循环直到 currentTimeMillis() > base
  private def waitNextMillis(base: Long): Long = {
    var timestamp = currentTimeMillis()
    while timestamp <= base do timestamp = currentTimeMillis()
    timestamp
  }
}

object SnowflakeIdGenerator {
  import java.time.Instant

  // 配置参数：各字段占用的二进制位数，三者相加 = 22 位（剩下的 41 位留给时间戳，1 位留给符号位）
  // 机器 ID 占 5 位，取值范围 0 ~ 31，最多支持 32 台机器
  private val WorkerIdBits     = 5L
  // 数据中心 ID 占 5 位，取值范围 0 ~ 31，最多支持 32 个数据中心
  private val DatacenterIdBits = 5L
  // 序列号占 12 位，取值范围 0 ~ 4095，即同一毫秒内最多生成 4096 个 ID
  private val SequenceBits     = 12L

  // 各字段的上限，用于范围校验
  // 1L << 5 = 32，即 workerId 必须 < 32（合法值 0~31）
  private val MaxWorkerId     = 1L << WorkerIdBits
  // 1L << 5 = 32，即 datacenterId 必须 < 32（合法值 0~31）
  private val MaxDatacenterId = 1L << DatacenterIdBits

  // 位移偏移量（拼接 ID 时各字段要左移的位数）
  // 序列号在最低位，不需要移位，占 bit 0~11
  private val WorkerIdShift     = SequenceBits // 12
  // 机器 ID 要跳过序列号，占 bit 12~16
  private val DatacenterIdShift = SequenceBits + WorkerIdBits // 12 + 5 = 17
  // 时间戳在最高位，要跳过序列号 + 机器 ID + 数据中心 ID，占 bit 22~62
  private val TimestampShift    = SequenceBits + WorkerIdBits + DatacenterIdBits // 12 + 5 + 5 = 22

  // 掩码，用于 sequence 自增后取低 12 位，溢出时自动归零，避免污染高位
  // (1L << 12) - 1L = 0b111111111111 = 4095
  private val SequenceMask = (1L << SequenceBits) - 1L

  // 自定义纪元：将“时间戳的起点”从 1970-01-01 挪到一个更近的时间点，
  // 比如此处示例 2020-01-01T00:00:00Z；这样时间戳这部分占用的位数更少，能支撑更久不溢出。
  // 注意：减纪元本身跟回拨处理无关，它只负责压缩位数。
  private val Epoch = 1577836800000L

  // 最终生成的 64 位 ID 布局示例：
  // bit 63    bit 62 ................... bit 22  bit 21  ... bit 17  bit 16 ... bit 12  bit 11 ... bit 0
  // +------+---------------------------+------------------+------------------+-------------------------+
  // | 符号  |     时间戳（41 位）        | 数据中心 ID（5位） | 机器 ID（5位）     |   序列号（12位）          |
  // |  0   |  (now - Epoch) << 22     | dcId << 17       | workerId << 12   |   sequence              |
  // +------+-------------------------+------------------+------------------+--------------------------+
  def apply(workerId: Long, datacenterId: Long): SnowflakeIdGenerator = new SnowflakeIdGenerator(workerId, datacenterId)

  // 获得当前时间戳
  private def currentTimeMillis(): Long = System.currentTimeMillis()

  /** 从雪花 ID 反解出的组成部分 */
  case class IdParts(
    timestamp: Long, // 绝对毫秒时间戳（已加回纪元 Epoch，是真正的 Unix 毫秒）
    instant: Instant, // timestamp 对应的 Instant（UTC 时间点，toString 会带 Z）
    datacenterId: Long, // 数据中心 ID，范围 0 ~ 31
    workerId: Long, // 机器 ID，范围 0 ~ 31
    sequence: Long // 同一毫秒内的序列号，范围 0 ~ 4095
  )

  /**
   * 把一个雪花 ID 拆解回它的各个组成部分。
   *
   * 生成时的公式：
   *  ((timestamp - Epoch) << TimestampShift)   // 22
   *     | (datacenterId << DatacenterIdShift)  // 17
   *     | (workerId     << WorkerIdShift)      // 12
   *     | sequence                             // 0
   * 反解即反过来：先右移定位，再用掩码截取对应位。
   *
   * @param id 要反解的雪花 ID
   * @return 拆解后的各字段
   */
  def parse(id: Long): IdParts = {
    // 序列号在最低 12 位，直接用掩码 0b111111111111 取出
    // 0 ~ 11 位，无需右移
    val sequence     = id & SequenceMask
    // 机器 ID 在 bit 12 ~ 16（共 5 位）
    // 先右移 12 位把它挪到最低位，再用 5 位掩码 (MaxWorkerId - 1 = 0b11111) 截取
    val workerId     = (id >>> WorkerIdShift.toInt) & (MaxWorkerId - 1)
    // 数据中心 ID 在 bit 17 ~ 21（共 5 位）
    // 先右移 17 位，再用 5 位掩码截取
    // 注意：DatacenterIdShift(17) 比 WorkerIdShift(12) 大，所以数据中心在机器 ID 的左边（高位）
    val datacenterId = (id >>> DatacenterIdShift.toInt) & (MaxDatacenterId - 1)
    // 时间戳在最高位 bit 22 ~ 62（共 41 位）
    // 右移 22 位后，剩下的就是生成时写入的“相对纪元的毫秒数”
    // 用 >>> 而不是 >>，是无符号右移，避免符号位被带进来污染结果
    val tsRelative   = id >>> TimestampShift.toInt
    // 关键一步：加回纪元，还原成真正的 Unix 毫秒
    // 生成时是 (now - Epoch)，所以反解必须 + Epoch
    // 漏掉这一步，得到的时间会早于 1970 年附近，明显不对
    val tsAbsolute   = tsRelative + Epoch

    IdParts(
      timestamp = tsAbsolute,
      // Instant.ofEpochMilli 接收的就是 Unix 毫秒，直接对应
      instant = Instant.ofEpochMilli(tsAbsolute),
      datacenterId = datacenterId,
      workerId = workerId,
      sequence = sequence
    )
  }
}
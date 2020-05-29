## Java总结篇

### Java基础知识



#### 基本数据类型

| 基本数据类型 | 初始值   | 所占空间大小（字节/byte） | 取值范围（1`byte`=8`bit`且数值类型都是有符号位的） | Cache                                                        |
| ------------ | -------- | ------------------------- | -------------------------------------------------- | ------------------------------------------------------------ |
| byte         | (byte)0  | 1                         | -2^7^~2^7^-1                                       | `Byte.ByteCache`（-128~127）                                 |
| char         | 0、`NUL` | 2                         | 0~2^16-1                                           | `Character.CharacterCache`（0~127）                          |
| short        | (short)0 | 2                         | -2^15^ ~  2^15^-1                                  | `Short.ShortCache`（-128~127）                               |
| int          | 0        | 4                         | -2^31^ ~  2^31^-1                                  | `Integer.IntegerCache`（-128~`high`）（默认`high`:127，可通过`-XX:AutoBoxCacheMax=<size>`指定） |
| float        | `0.0f`   | 4                         |                                                    | 无                                                           |
| double       | `0.0d`   | 8                         |                                                    | 无                                                           |
| long         | `0L`     | 8                         | -2^63^ ~  2^63^-1                                  | `Long.LongCache`（-128~127）                                 |
| boolean      | false    | 1                         |                                                    | 无                                                           |



注意1：`String`不是基本数据类型，是引用类型。

注意2：使用`BigInteger`、`BigDecimal`或者Long（单位为分）表示金额等重要指标，因为浮点数不是准确值。

注意3：在`Java SE5`，Java提供了自动拆装箱的机制。例如Integer的自动拆箱是通过`java.lang.Integer#intValue()`完成的，而自动装箱是通过`java.lang.Integer#valueOf(int)`完成的。
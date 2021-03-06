// Code generated by Wire protocol buffer compiler, do not edit.
// Source: fsutil.types.Stat in github.com/tonistiigi/fsutil/types/stat.proto
package fsutil.types

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.Syntax.PROTO_3
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.immutableCopyOf
import com.squareup.wire.`internal`.sanitize
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Long
import kotlin.Nothing
import kotlin.String
import kotlin.Unit
import kotlin.collections.Map
import kotlin.hashCode
import kotlin.jvm.JvmField
import kotlin.lazy
import okio.ByteString

public class Stat(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    label = WireField.Label.OMIT_IDENTITY
  )
  public val path: String = "",
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#UINT32",
    label = WireField.Label.OMIT_IDENTITY
  )
  public val mode: Int = 0,
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#UINT32",
    label = WireField.Label.OMIT_IDENTITY
  )
  public val uid: Int = 0,
  @field:WireField(
    tag = 4,
    adapter = "com.squareup.wire.ProtoAdapter#UINT32",
    label = WireField.Label.OMIT_IDENTITY
  )
  public val gid: Int = 0,
  @field:WireField(
    tag = 5,
    adapter = "com.squareup.wire.ProtoAdapter#INT64",
    label = WireField.Label.OMIT_IDENTITY
  )
  public val size: Long = 0L,
  @field:WireField(
    tag = 6,
    adapter = "com.squareup.wire.ProtoAdapter#INT64",
    label = WireField.Label.OMIT_IDENTITY
  )
  public val modTime: Long = 0L,
  /**
   * int32 typeflag = 7;
   */
  @field:WireField(
    tag = 7,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    label = WireField.Label.OMIT_IDENTITY
  )
  public val linkname: String = "",
  @field:WireField(
    tag = 8,
    adapter = "com.squareup.wire.ProtoAdapter#INT64",
    label = WireField.Label.OMIT_IDENTITY
  )
  public val devmajor: Long = 0L,
  @field:WireField(
    tag = 9,
    adapter = "com.squareup.wire.ProtoAdapter#INT64",
    label = WireField.Label.OMIT_IDENTITY
  )
  public val devminor: Long = 0L,
  xattrs: Map<String, ByteString> = emptyMap(),
  unknownFields: ByteString = ByteString.EMPTY
) : Message<Stat, Nothing>(ADAPTER, unknownFields) {
  @field:WireField(
    tag = 10,
    keyAdapter = "com.squareup.wire.ProtoAdapter#STRING",
    adapter = "com.squareup.wire.ProtoAdapter#BYTES"
  )
  public val xattrs: Map<String, ByteString> = immutableCopyOf("xattrs", xattrs)

  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  public override fun newBuilder(): Nothing = throw AssertionError()

  public override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is Stat) return false
    if (unknownFields != other.unknownFields) return false
    if (path != other.path) return false
    if (mode != other.mode) return false
    if (uid != other.uid) return false
    if (gid != other.gid) return false
    if (size != other.size) return false
    if (modTime != other.modTime) return false
    if (linkname != other.linkname) return false
    if (devmajor != other.devmajor) return false
    if (devminor != other.devminor) return false
    if (xattrs != other.xattrs) return false
    return true
  }

  public override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + path.hashCode()
      result = result * 37 + mode.hashCode()
      result = result * 37 + uid.hashCode()
      result = result * 37 + gid.hashCode()
      result = result * 37 + size.hashCode()
      result = result * 37 + modTime.hashCode()
      result = result * 37 + linkname.hashCode()
      result = result * 37 + devmajor.hashCode()
      result = result * 37 + devminor.hashCode()
      result = result * 37 + xattrs.hashCode()
      super.hashCode = result
    }
    return result
  }

  public override fun toString(): String {
    val result = mutableListOf<String>()
    result += """path=${sanitize(path)}"""
    result += """mode=$mode"""
    result += """uid=$uid"""
    result += """gid=$gid"""
    result += """size=$size"""
    result += """modTime=$modTime"""
    result += """linkname=${sanitize(linkname)}"""
    result += """devmajor=$devmajor"""
    result += """devminor=$devminor"""
    if (xattrs.isNotEmpty()) result += """xattrs=$xattrs"""
    return result.joinToString(prefix = "Stat{", separator = ", ", postfix = "}")
  }

  public fun copy(
    path: String = this.path,
    mode: Int = this.mode,
    uid: Int = this.uid,
    gid: Int = this.gid,
    size: Long = this.size,
    modTime: Long = this.modTime,
    linkname: String = this.linkname,
    devmajor: Long = this.devmajor,
    devminor: Long = this.devminor,
    xattrs: Map<String, ByteString> = this.xattrs,
    unknownFields: ByteString = this.unknownFields
  ): Stat = Stat(path, mode, uid, gid, size, modTime, linkname, devmajor, devminor, xattrs,
      unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<Stat> = object : ProtoAdapter<Stat>(
      FieldEncoding.LENGTH_DELIMITED, 
      Stat::class, 
      "type.googleapis.com/fsutil.types.Stat", 
      PROTO_3, 
      null
    ) {
      private val xattrsAdapter: ProtoAdapter<Map<String, ByteString>> by lazy {
          ProtoAdapter.newMapAdapter(ProtoAdapter.STRING, ProtoAdapter.BYTES) }

      public override fun encodedSize(value: Stat): Int {
        var size_ = value.unknownFields.size
        if (value.path != "") size_ += ProtoAdapter.STRING.encodedSizeWithTag(1, value.path)
        if (value.mode != 0) size_ += ProtoAdapter.UINT32.encodedSizeWithTag(2, value.mode)
        if (value.uid != 0) size_ += ProtoAdapter.UINT32.encodedSizeWithTag(3, value.uid)
        if (value.gid != 0) size_ += ProtoAdapter.UINT32.encodedSizeWithTag(4, value.gid)
        if (value.size != 0L) size_ += ProtoAdapter.INT64.encodedSizeWithTag(5, value.size)
        if (value.modTime != 0L) size_ += ProtoAdapter.INT64.encodedSizeWithTag(6, value.modTime)
        if (value.linkname != "") size_ += ProtoAdapter.STRING.encodedSizeWithTag(7, value.linkname)
        if (value.devmajor != 0L) size_ += ProtoAdapter.INT64.encodedSizeWithTag(8, value.devmajor)
        if (value.devminor != 0L) size_ += ProtoAdapter.INT64.encodedSizeWithTag(9, value.devminor)
        size_ += xattrsAdapter.encodedSizeWithTag(10, value.xattrs)
        return size_
      }

      public override fun encode(writer: ProtoWriter, value: Stat): Unit {
        if (value.path != "") ProtoAdapter.STRING.encodeWithTag(writer, 1, value.path)
        if (value.mode != 0) ProtoAdapter.UINT32.encodeWithTag(writer, 2, value.mode)
        if (value.uid != 0) ProtoAdapter.UINT32.encodeWithTag(writer, 3, value.uid)
        if (value.gid != 0) ProtoAdapter.UINT32.encodeWithTag(writer, 4, value.gid)
        if (value.size != 0L) ProtoAdapter.INT64.encodeWithTag(writer, 5, value.size)
        if (value.modTime != 0L) ProtoAdapter.INT64.encodeWithTag(writer, 6, value.modTime)
        if (value.linkname != "") ProtoAdapter.STRING.encodeWithTag(writer, 7, value.linkname)
        if (value.devmajor != 0L) ProtoAdapter.INT64.encodeWithTag(writer, 8, value.devmajor)
        if (value.devminor != 0L) ProtoAdapter.INT64.encodeWithTag(writer, 9, value.devminor)
        xattrsAdapter.encodeWithTag(writer, 10, value.xattrs)
        writer.writeBytes(value.unknownFields)
      }

      public override fun decode(reader: ProtoReader): Stat {
        var path: String = ""
        var mode: Int = 0
        var uid: Int = 0
        var gid: Int = 0
        var size: Long = 0L
        var modTime: Long = 0L
        var linkname: String = ""
        var devmajor: Long = 0L
        var devminor: Long = 0L
        val xattrs = mutableMapOf<String, ByteString>()
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> path = ProtoAdapter.STRING.decode(reader)
            2 -> mode = ProtoAdapter.UINT32.decode(reader)
            3 -> uid = ProtoAdapter.UINT32.decode(reader)
            4 -> gid = ProtoAdapter.UINT32.decode(reader)
            5 -> size = ProtoAdapter.INT64.decode(reader)
            6 -> modTime = ProtoAdapter.INT64.decode(reader)
            7 -> linkname = ProtoAdapter.STRING.decode(reader)
            8 -> devmajor = ProtoAdapter.INT64.decode(reader)
            9 -> devminor = ProtoAdapter.INT64.decode(reader)
            10 -> xattrs.putAll(xattrsAdapter.decode(reader))
            else -> reader.readUnknownField(tag)
          }
        }
        return Stat(
          path = path,
          mode = mode,
          uid = uid,
          gid = gid,
          size = size,
          modTime = modTime,
          linkname = linkname,
          devmajor = devmajor,
          devminor = devminor,
          xattrs = xattrs,
          unknownFields = unknownFields
        )
      }

      public override fun redact(value: Stat): Stat = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}

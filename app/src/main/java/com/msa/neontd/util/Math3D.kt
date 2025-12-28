package com.msa.neontd.util

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D Vector class for 3D rendering and physics.
 * Follows the same mutable pattern as Vector2 for consistency.
 */
data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {

    fun set(x: Float, y: Float, z: Float): Vector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(other: Vector3): Vector3 {
        this.x = other.x
        this.y = other.y
        this.z = other.z
        return this
    }

    fun add(other: Vector3): Vector3 {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    fun add(dx: Float, dy: Float, dz: Float): Vector3 {
        x += dx
        y += dy
        z += dz
        return this
    }

    fun sub(other: Vector3): Vector3 {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }

    fun mul(scalar: Float): Vector3 {
        x *= scalar
        y *= scalar
        z *= scalar
        return this
    }

    fun div(scalar: Float): Vector3 {
        x /= scalar
        y /= scalar
        z /= scalar
        return this
    }

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalize(): Vector3 {
        val len = length()
        if (len != 0f) {
            x /= len
            y /= len
            z /= len
        }
        return this
    }

    fun normalized(): Vector3 = copy().normalize()

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 {
        val cx = y * other.z - z * other.y
        val cy = z * other.x - x * other.z
        val cz = x * other.y - y * other.x
        return Vector3(cx, cy, cz)
    }

    fun distance(other: Vector3): Float {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun distanceSquared(other: Vector3): Float {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        return dx * dx + dy * dy + dz * dz
    }

    fun lerp(target: Vector3, t: Float): Vector3 {
        x += (target.x - x) * t
        y += (target.y - y) * t
        z += (target.z - z) * t
        return this
    }

    fun negate(): Vector3 {
        x = -x
        y = -y
        z = -z
        return this
    }

    fun copy(): Vector3 = Vector3(x, y, z)

    fun toVector2(): Vector2 = Vector2(x, y)

    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun unaryMinus(): Vector3 = Vector3(-x, -y, -z)

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val UP = Vector3(0f, 1f, 0f)
        val DOWN = Vector3(0f, -1f, 0f)
        val LEFT = Vector3(-1f, 0f, 0f)
        val RIGHT = Vector3(1f, 0f, 0f)
        val FORWARD = Vector3(0f, 0f, -1f)
        val BACK = Vector3(0f, 0f, 1f)

        fun fromVector2(v: Vector2, z: Float = 0f): Vector3 = Vector3(v.x, v.y, z)
    }
}

/**
 * Quaternion for 3D rotations.
 * Uses (x, y, z, w) convention where w is the scalar component.
 */
data class Quaternion(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var w: Float = 1f
) {
    fun set(x: Float, y: Float, z: Float, w: Float): Quaternion {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    fun set(other: Quaternion): Quaternion {
        this.x = other.x
        this.y = other.y
        this.z = other.z
        this.w = other.w
        return this
    }

    fun setIdentity(): Quaternion {
        x = 0f
        y = 0f
        z = 0f
        w = 1f
        return this
    }

    fun length(): Float = sqrt(x * x + y * y + z * z + w * w)

    fun normalize(): Quaternion {
        val len = length()
        if (len != 0f) {
            x /= len
            y /= len
            z /= len
            w /= len
        }
        return this
    }

    fun conjugate(): Quaternion = Quaternion(-x, -y, -z, w)

    fun inverse(): Quaternion {
        val lenSq = x * x + y * y + z * z + w * w
        return Quaternion(-x / lenSq, -y / lenSq, -z / lenSq, w / lenSq)
    }

    /**
     * Multiplies this quaternion by another (applies rotation).
     * Result represents: this rotation followed by other rotation.
     */
    fun multiply(other: Quaternion): Quaternion {
        val nx = w * other.x + x * other.w + y * other.z - z * other.y
        val ny = w * other.y - x * other.z + y * other.w + z * other.x
        val nz = w * other.z + x * other.y - y * other.x + z * other.w
        val nw = w * other.w - x * other.x - y * other.y - z * other.z
        return Quaternion(nx, ny, nz, nw)
    }

    /**
     * Rotates a vector by this quaternion.
     */
    fun rotateVector(v: Vector3): Vector3 {
        val qv = Quaternion(v.x, v.y, v.z, 0f)
        val result = this.multiply(qv).multiply(this.conjugate())
        return Vector3(result.x, result.y, result.z)
    }

    /**
     * Spherical linear interpolation between quaternions.
     */
    fun slerp(target: Quaternion, t: Float): Quaternion {
        var dot = x * target.x + y * target.y + z * target.z + w * target.w

        // Handle negative dot (shortest path)
        val negated = if (dot < 0f) {
            dot = -dot
            true
        } else false

        val (scale0, scale1) = if (dot > 0.9995f) {
            // Linear interpolation for close quaternions
            Pair(1f - t, t)
        } else {
            val theta = acos(dot.coerceIn(-1f, 1f))
            val sinTheta = sin(theta)
            Pair(sin((1f - t) * theta) / sinTheta, sin(t * theta) / sinTheta)
        }

        val s1 = if (negated) -scale1 else scale1

        return Quaternion(
            scale0 * x + s1 * target.x,
            scale0 * y + s1 * target.y,
            scale0 * z + s1 * target.z,
            scale0 * w + s1 * target.w
        ).normalize()
    }

    fun copy(): Quaternion = Quaternion(x, y, z, w)

    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)

        /**
         * Creates a quaternion from Euler angles (in radians).
         * Order: pitch (X), yaw (Y), roll (Z)
         */
        fun fromEuler(pitch: Float, yaw: Float, roll: Float): Quaternion {
            val cy = cos(yaw * 0.5f)
            val sy = sin(yaw * 0.5f)
            val cp = cos(pitch * 0.5f)
            val sp = sin(pitch * 0.5f)
            val cr = cos(roll * 0.5f)
            val sr = sin(roll * 0.5f)

            return Quaternion(
                x = sr * cp * cy - cr * sp * sy,
                y = cr * sp * cy + sr * cp * sy,
                z = cr * cp * sy - sr * sp * cy,
                w = cr * cp * cy + sr * sp * sy
            )
        }

        /**
         * Creates a quaternion from axis-angle representation.
         */
        fun fromAxisAngle(axis: Vector3, angleRadians: Float): Quaternion {
            val halfAngle = angleRadians * 0.5f
            val s = sin(halfAngle)
            val normalizedAxis = axis.normalized()
            return Quaternion(
                normalizedAxis.x * s,
                normalizedAxis.y * s,
                normalizedAxis.z * s,
                cos(halfAngle)
            )
        }

        /**
         * Creates a quaternion that rotates from one direction to another.
         */
        fun fromToRotation(from: Vector3, to: Vector3): Quaternion {
            val f = from.normalized()
            val t = to.normalized()
            val dot = f.dot(t)

            if (dot > 0.9999f) return IDENTITY.copy()
            if (dot < -0.9999f) {
                // 180 degree rotation around any perpendicular axis
                var axis = Vector3.RIGHT.copy().cross(f)
                if (axis.lengthSquared() < 0.0001f) {
                    axis = Vector3.UP.copy().cross(f)
                }
                return fromAxisAngle(axis.normalize(), Math.PI.toFloat())
            }

            val axis = f.cross(t)
            return Quaternion(
                axis.x,
                axis.y,
                axis.z,
                1f + dot
            ).normalize()
        }
    }
}

/**
 * 4x4 Matrix for 3D transformations.
 * Uses column-major order for OpenGL compatibility.
 *
 * Layout:
 * | m0  m4  m8   m12 |   | Xx  Yx  Zx  Tx |
 * | m1  m5  m9   m13 | = | Xy  Yy  Zy  Ty |
 * | m2  m6  m10  m14 |   | Xz  Yz  Zz  Tz |
 * | m3  m7  m11  m15 |   | 0   0   0   1  |
 */
class Matrix4x4 {
    val data = FloatArray(16)

    init {
        setIdentity()
    }

    fun setIdentity(): Matrix4x4 {
        data.fill(0f)
        data[0] = 1f
        data[5] = 1f
        data[10] = 1f
        data[15] = 1f
        return this
    }

    fun set(other: Matrix4x4): Matrix4x4 {
        System.arraycopy(other.data, 0, data, 0, 16)
        return this
    }

    /**
     * Sets perspective projection matrix.
     */
    fun setPerspective(fovYRadians: Float, aspect: Float, near: Float, far: Float): Matrix4x4 {
        val f = 1f / kotlin.math.tan(fovYRadians / 2f)
        val rangeInv = 1f / (near - far)

        data.fill(0f)
        data[0] = f / aspect
        data[5] = f
        data[10] = (near + far) * rangeInv
        data[11] = -1f
        data[14] = 2f * near * far * rangeInv
        return this
    }

    /**
     * Sets orthographic projection matrix.
     */
    fun setOrthographic(
        left: Float, right: Float,
        bottom: Float, top: Float,
        near: Float, far: Float
    ): Matrix4x4 {
        data.fill(0f)
        data[0] = 2f / (right - left)
        data[5] = 2f / (top - bottom)
        data[10] = -2f / (far - near)
        data[12] = -(right + left) / (right - left)
        data[13] = -(top + bottom) / (top - bottom)
        data[14] = -(far + near) / (far - near)
        data[15] = 1f
        return this
    }

    /**
     * Sets view matrix (camera look-at).
     */
    fun setLookAt(eye: Vector3, center: Vector3, up: Vector3): Matrix4x4 {
        val f = Vector3(center.x - eye.x, center.y - eye.y, center.z - eye.z).normalize()
        val s = f.cross(up).normalize()
        val u = s.cross(f)

        data[0] = s.x
        data[4] = s.y
        data[8] = s.z
        data[12] = -s.dot(eye)

        data[1] = u.x
        data[5] = u.y
        data[9] = u.z
        data[13] = -u.dot(eye)

        data[2] = -f.x
        data[6] = -f.y
        data[10] = -f.z
        data[14] = f.dot(eye)

        data[3] = 0f
        data[7] = 0f
        data[11] = 0f
        data[15] = 1f

        return this
    }

    /**
     * Sets translation matrix.
     */
    fun setTranslation(x: Float, y: Float, z: Float): Matrix4x4 {
        setIdentity()
        data[12] = x
        data[13] = y
        data[14] = z
        return this
    }

    fun setTranslation(v: Vector3): Matrix4x4 = setTranslation(v.x, v.y, v.z)

    /**
     * Sets rotation matrix from quaternion.
     */
    fun setRotation(q: Quaternion): Matrix4x4 {
        val x2 = q.x * 2f
        val y2 = q.y * 2f
        val z2 = q.z * 2f
        val xx = q.x * x2
        val xy = q.x * y2
        val xz = q.x * z2
        val yy = q.y * y2
        val yz = q.y * z2
        val zz = q.z * z2
        val wx = q.w * x2
        val wy = q.w * y2
        val wz = q.w * z2

        data[0] = 1f - (yy + zz)
        data[1] = xy + wz
        data[2] = xz - wy
        data[3] = 0f

        data[4] = xy - wz
        data[5] = 1f - (xx + zz)
        data[6] = yz + wx
        data[7] = 0f

        data[8] = xz + wy
        data[9] = yz - wx
        data[10] = 1f - (xx + yy)
        data[11] = 0f

        data[12] = 0f
        data[13] = 0f
        data[14] = 0f
        data[15] = 1f

        return this
    }

    /**
     * Sets scale matrix.
     */
    fun setScale(x: Float, y: Float, z: Float): Matrix4x4 {
        setIdentity()
        data[0] = x
        data[5] = y
        data[10] = z
        return this
    }

    fun setScale(v: Vector3): Matrix4x4 = setScale(v.x, v.y, v.z)

    /**
     * Sets TRS (Translation-Rotation-Scale) transform matrix.
     */
    fun setTRS(translation: Vector3, rotation: Quaternion, scale: Vector3): Matrix4x4 {
        // Start with rotation
        setRotation(rotation)

        // Apply scale to rotation columns
        data[0] *= scale.x
        data[1] *= scale.x
        data[2] *= scale.x
        data[4] *= scale.y
        data[5] *= scale.y
        data[6] *= scale.y
        data[8] *= scale.z
        data[9] *= scale.z
        data[10] *= scale.z

        // Set translation
        data[12] = translation.x
        data[13] = translation.y
        data[14] = translation.z

        return this
    }

    /**
     * Multiplies this matrix by another: result = this * other
     */
    fun multiply(other: Matrix4x4): Matrix4x4 {
        val result = Matrix4x4()
        val a = this.data
        val b = other.data
        val r = result.data

        for (col in 0..3) {
            for (row in 0..3) {
                val i = col * 4 + row
                r[i] = a[row] * b[col * 4] +
                        a[4 + row] * b[col * 4 + 1] +
                        a[8 + row] * b[col * 4 + 2] +
                        a[12 + row] * b[col * 4 + 3]
            }
        }
        return result
    }

    operator fun times(other: Matrix4x4): Matrix4x4 = multiply(other)

    /**
     * Transforms a point by this matrix.
     */
    fun transformPoint(point: Vector3): Vector3 {
        val x = data[0] * point.x + data[4] * point.y + data[8] * point.z + data[12]
        val y = data[1] * point.x + data[5] * point.y + data[9] * point.z + data[13]
        val z = data[2] * point.x + data[6] * point.y + data[10] * point.z + data[14]
        val w = data[3] * point.x + data[7] * point.y + data[11] * point.z + data[15]
        return if (w != 0f && w != 1f) {
            Vector3(x / w, y / w, z / w)
        } else {
            Vector3(x, y, z)
        }
    }

    /**
     * Transforms a direction vector (ignores translation).
     */
    fun transformDirection(dir: Vector3): Vector3 {
        return Vector3(
            data[0] * dir.x + data[4] * dir.y + data[8] * dir.z,
            data[1] * dir.x + data[5] * dir.y + data[9] * dir.z,
            data[2] * dir.x + data[6] * dir.y + data[10] * dir.z
        )
    }

    /**
     * Calculates the inverse of this matrix.
     */
    fun inverse(): Matrix4x4 {
        val result = Matrix4x4()
        val m = data
        val inv = result.data

        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10]
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10]
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9]
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9]
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10]
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10]
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9]
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9]
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6]
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6]
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5]
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5]
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6]
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6]
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5]
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5]

        var det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12]
        if (abs(det) < 0.00001f) {
            return Matrix4x4() // Return identity if not invertible
        }
        det = 1f / det
        for (i in 0..15) {
            inv[i] *= det
        }
        return result
    }

    /**
     * Transposes this matrix.
     */
    fun transpose(): Matrix4x4 {
        val result = Matrix4x4()
        for (row in 0..3) {
            for (col in 0..3) {
                result.data[col * 4 + row] = data[row * 4 + col]
            }
        }
        return result
    }

    fun copy(): Matrix4x4 = Matrix4x4().also { it.set(this) }

    /**
     * Applies a translation to this matrix (post-multiply).
     * M = M * T
     */
    fun translate(x: Float, y: Float, z: Float): Matrix4x4 {
        data[12] += data[0] * x + data[4] * y + data[8] * z
        data[13] += data[1] * x + data[5] * y + data[9] * z
        data[14] += data[2] * x + data[6] * y + data[10] * z
        data[15] += data[3] * x + data[7] * y + data[11] * z
        return this
    }

    fun translate(v: Vector3): Matrix4x4 = translate(v.x, v.y, v.z)

    /**
     * Applies an X-axis rotation to this matrix (post-multiply).
     * Rotation is in degrees.
     * M = M * Rx
     */
    fun rotateX(degrees: Float): Matrix4x4 {
        val radians = degrees * (Math.PI.toFloat() / 180f)
        val c = cos(radians)
        val s = sin(radians)

        // Save current values (Y and Z columns are affected)
        val m4 = data[4]; val m5 = data[5]; val m6 = data[6]; val m7 = data[7]
        val m8 = data[8]; val m9 = data[9]; val m10 = data[10]; val m11 = data[11]

        // Apply rotation: Y' = Y*cos - Z*sin, Z' = Y*sin + Z*cos
        data[4] = m4 * c + m8 * s
        data[5] = m5 * c + m9 * s
        data[6] = m6 * c + m10 * s
        data[7] = m7 * c + m11 * s
        data[8] = m4 * (-s) + m8 * c
        data[9] = m5 * (-s) + m9 * c
        data[10] = m6 * (-s) + m10 * c
        data[11] = m7 * (-s) + m11 * c

        return this
    }

    /**
     * Applies a Y-axis rotation to this matrix (post-multiply).
     * Rotation is in degrees.
     * M = M * Ry
     */
    fun rotateY(degrees: Float): Matrix4x4 {
        val radians = degrees * (Math.PI.toFloat() / 180f)
        val c = cos(radians)
        val s = sin(radians)

        // Save current values
        val m0 = data[0]; val m1 = data[1]; val m2 = data[2]; val m3 = data[3]
        val m8 = data[8]; val m9 = data[9]; val m10 = data[10]; val m11 = data[11]

        // Apply rotation
        data[0] = m0 * c + m8 * (-s)
        data[1] = m1 * c + m9 * (-s)
        data[2] = m2 * c + m10 * (-s)
        data[3] = m3 * c + m11 * (-s)
        data[8] = m0 * s + m8 * c
        data[9] = m1 * s + m9 * c
        data[10] = m2 * s + m10 * c
        data[11] = m3 * s + m11 * c

        return this
    }

    /**
     * Applies a Z-axis rotation to this matrix (post-multiply).
     * Rotation is in degrees.
     * M = M * Rz
     */
    fun rotateZ(degrees: Float): Matrix4x4 {
        val radians = degrees * (Math.PI.toFloat() / 180f)
        val c = cos(radians)
        val s = sin(radians)

        // Save current values (X and Y columns are affected)
        val m0 = data[0]; val m1 = data[1]; val m2 = data[2]; val m3 = data[3]
        val m4 = data[4]; val m5 = data[5]; val m6 = data[6]; val m7 = data[7]

        // Apply rotation: X' = X*cos - Y*sin, Y' = X*sin + Y*cos
        data[0] = m0 * c + m4 * s
        data[1] = m1 * c + m5 * s
        data[2] = m2 * c + m6 * s
        data[3] = m3 * c + m7 * s
        data[4] = m0 * (-s) + m4 * c
        data[5] = m1 * (-s) + m5 * c
        data[6] = m2 * (-s) + m6 * c
        data[7] = m3 * (-s) + m7 * c

        return this
    }

    /**
     * Applies a uniform or non-uniform scale to this matrix (post-multiply).
     * M = M * S
     */
    fun scale(x: Float, y: Float, z: Float): Matrix4x4 {
        data[0] *= x; data[1] *= x; data[2] *= x; data[3] *= x
        data[4] *= y; data[5] *= y; data[6] *= y; data[7] *= y
        data[8] *= z; data[9] *= z; data[10] *= z; data[11] *= z
        return this
    }

    fun scale(uniform: Float): Matrix4x4 = scale(uniform, uniform, uniform)
    fun scale(v: Vector3): Matrix4x4 = scale(v.x, v.y, v.z)

    companion object {
        fun identity(): Matrix4x4 = Matrix4x4()

        fun translation(x: Float, y: Float, z: Float): Matrix4x4 = Matrix4x4().setTranslation(x, y, z)
        fun translation(v: Vector3): Matrix4x4 = translation(v.x, v.y, v.z)

        fun rotation(q: Quaternion): Matrix4x4 = Matrix4x4().setRotation(q)

        fun scale(x: Float, y: Float, z: Float): Matrix4x4 = Matrix4x4().setScale(x, y, z)
        fun scale(v: Vector3): Matrix4x4 = scale(v.x, v.y, v.z)
        fun scale(uniform: Float): Matrix4x4 = scale(uniform, uniform, uniform)

        fun perspective(fovYRadians: Float, aspect: Float, near: Float, far: Float): Matrix4x4 =
            Matrix4x4().setPerspective(fovYRadians, aspect, near, far)

        fun orthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4x4 =
            Matrix4x4().setOrthographic(left, right, bottom, top, near, far)

        fun lookAt(eye: Vector3, center: Vector3, up: Vector3): Matrix4x4 =
            Matrix4x4().setLookAt(eye, center, up)

        fun trs(translation: Vector3, rotation: Quaternion, scale: Vector3): Matrix4x4 =
            Matrix4x4().setTRS(translation, rotation, scale)
    }
}

/**
 * Plane representation for frustum culling.
 */
data class Plane(
    var normal: Vector3 = Vector3.UP.copy(),
    var distance: Float = 0f
) {
    fun distanceToPoint(point: Vector3): Float {
        return normal.dot(point) + distance
    }

    fun normalize(): Plane {
        val len = normal.length()
        if (len != 0f) {
            normal.div(len)
            distance /= len
        }
        return this
    }

    companion object {
        val XY_PLANE = Plane(Vector3.FORWARD.copy(), 0f)
        val XZ_PLANE = Plane(Vector3.UP.copy(), 0f)
        val YZ_PLANE = Plane(Vector3.RIGHT.copy(), 0f)
    }
}

/**
 * Ray for raycasting and intersection tests.
 */
data class Ray(
    val origin: Vector3,
    val direction: Vector3
) {
    fun getPoint(t: Float): Vector3 = origin + direction * t

    /**
     * Intersects ray with a plane. Returns the intersection point or null.
     */
    fun intersectPlane(plane: Plane): Vector3? {
        val denom = plane.normal.dot(direction)
        if (abs(denom) < 0.0001f) return null

        val t = -(plane.normal.dot(origin) + plane.distance) / denom
        if (t < 0) return null

        return getPoint(t)
    }

    /**
     * Intersects ray with a sphere. Returns distance to nearest intersection or null.
     */
    fun intersectSphere(center: Vector3, radius: Float): Float? {
        val oc = origin - center
        val a = direction.dot(direction)
        val b = 2f * oc.dot(direction)
        val c = oc.dot(oc) - radius * radius
        val discriminant = b * b - 4f * a * c

        if (discriminant < 0) return null

        val t = (-b - sqrt(discriminant)) / (2f * a)
        return if (t >= 0) t else null
    }
}

/**
 * Bounding sphere for simple collision and culling.
 */
data class BoundingSphere(
    val center: Vector3,
    val radius: Float
) {
    fun contains(point: Vector3): Boolean {
        return center.distanceSquared(point) <= radius * radius
    }

    fun intersects(other: BoundingSphere): Boolean {
        val combinedRadius = radius + other.radius
        return center.distanceSquared(other.center) <= combinedRadius * combinedRadius
    }
}

/**
 * Axis-aligned bounding box for collision and culling.
 */
data class AABB(
    var min: Vector3 = Vector3.ZERO.copy(),
    var max: Vector3 = Vector3.ONE.copy()
) {
    val center: Vector3
        get() = Vector3(
            (min.x + max.x) * 0.5f,
            (min.y + max.y) * 0.5f,
            (min.z + max.z) * 0.5f
        )

    val size: Vector3
        get() = Vector3(
            max.x - min.x,
            max.y - min.y,
            max.z - min.z
        )

    fun contains(point: Vector3): Boolean {
        return point.x >= min.x && point.x <= max.x &&
                point.y >= min.y && point.y <= max.y &&
                point.z >= min.z && point.z <= max.z
    }

    fun intersects(other: AABB): Boolean {
        return min.x <= other.max.x && max.x >= other.min.x &&
                min.y <= other.max.y && max.y >= other.min.y &&
                min.z <= other.max.z && max.z >= other.min.z
    }

    fun toBoundingSphere(): BoundingSphere {
        val c = center
        val r = c.distance(max)
        return BoundingSphere(c, r)
    }
}

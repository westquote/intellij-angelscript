namespace Geometry
{
    const float Epsilon = 0.0001f;

    enum Axis
    {
        X,
        Y,
        Z
    }

    class Vector
    {
        float x;
        float y;
        float z;

        Vector() {}
        Vector(float x, float y, float z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        float Length() const
        {
            return sqrt(x * x + y * y + z * z);
        }

        float get_Size() const property { return Length(); }

        float opIndex(Axis axis) const
        {
            switch (axis)
            {
            case Axis::X: return x;
            case Axis::Y: return y;
            default: return z;
            }
        }
    }
}

#include "shared/geometry.as"

interface IShape
{
    float Area() const;
}

funcdef void Visitor(IShape@ shape);

int g_Count = 0;

[Editable]
class Circle : IShape
{
    private float m_Radius = 1.0f;

    Circle(float radius)
    {
        m_Radius = radius;
    }

    float Area() const override
    {
        return 3.14159f * m_Radius * m_Radius;
    }

    float Radius
    {
        get const { return m_Radius; }
        set { m_Radius = value; }
    }
}

void VisitAll(array<IShape@>@ shapes, Visitor@ visitor)
{
    for (uint i = 0; i < shapes.length(); i++)
    {
        visitor(shapes[i]);
    }
}

void Report(IShape@ shape)
{
    g_Count++;
    string text = "Area: ";
    print(text);
}

void Main()
{
    array<IShape@> shapes;
    shapes.insertLast(Circle(2.0f));
    VisitAll(shapes, Report);

    Geometry::Vector v(1, 2, 3);
    float length = v.Length() + v.Size + v[Geometry::Axis::Y];
    int mask = 0xFF & 0b1010 | 0o17;
    double ratio = 1.5e-3;
    string quote = """multi
line""";
    if (length > Geometry::Epsilon && mask != 0)
    {
        print("ok");
    }
}

#include "shared/geometry.as"
#define LONG_MACRO 1 + \
    2
#if EDITOR
#endif

/* Block comment
   over two lines */
// Line comment

[Editable, Category("Motion [fast]")]
class Mover : IMover
{
    [Meta] private float m_Speed = 1.5f;
    protected int64 m_Mask = 0xFF'FF & 0b1010 | 0o17 ^ 0d99;
    double m_Small = .5e-3;
    float m_Half = 2.f;

    int Speed
    {
        get const { return int(m_Speed); }
        set { m_Speed = value; }
    }

    void Update(const float &in dt, array<int>@ items) override final
    {
        for (uint i = 0; i < items.length(); i++)
        {
            items[i] **= 2;
            items[i] >>>= 1;
            if (items[i] !is null && !(items[i] >= 3) || items[i] <= -1 ^^ true)
                continue;
        }
        foreach (int item : items) { }
        switch (m_Mask) { case 1: break; default: return; }
        string text = """heredoc
with "quotes" inside""";
        string single = 'c';
        string escaped = "tab\t \"quoted\"";
        Geometry::Vector v;
        auto handle = @v;
        bool ok = cast<IMover>(this) is null ? false : true;
        try { } catch { }
    }
}

funcdef void Callback(int);
namespace Outer { enum Axis { X, Y } }
shared interface IMover { }
mixin class Mixin { }
import void External() from "other";
typedef float real;
using namespace Outer;
` $
"unterminated string
'also unterminated
/* unterminated block comment

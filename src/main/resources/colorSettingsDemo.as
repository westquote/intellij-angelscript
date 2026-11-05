#include "shared/math.as"

/* Block comment */
<NAMESPACE>Geometry</NAMESPACE>::<CLASS>Vector</CLASS> <LOCAL_VARIABLE>origin</LOCAL_VARIABLE>;

interface <INTERFACE>IShape</INTERFACE> { float <METHOD_CALL>Area</METHOD_CALL>() const; }

enum <ENUM>Axis</ENUM> { <ENUM_MEMBER>X</ENUM_MEMBER>, <ENUM_MEMBER>Y</ENUM_MEMBER> }

[Editable]
class <CLASS>Circle</CLASS> : <INTERFACE>IShape</INTERFACE>
{
    private float <FIELD>m_Radius</FIELD> = 1.5f;

    float <METHOD_CALL>Area</METHOD_CALL>() const override { return 3.14f * <FIELD>m_Radius</FIELD>; }
}

<TYPE>string</TYPE> <LOCAL_VARIABLE>title</LOCAL_VARIABLE>;
class <CLASS>Box</CLASS> { <TYPE_PARAMETER>T</TYPE_PARAMETER> <FIELD>value</FIELD>; }

// Line comment
void <FUNCTION_CALL>Report</FUNCTION_CALL>(<INTERFACE>IShape</INTERFACE>@ <PARAMETER>shape</PARAMETER>, int <PARAMETER>count</PARAMETER>)
{
    for (int <LOCAL_VARIABLE>i</LOCAL_VARIABLE> = 0x10; <LOCAL_VARIABLE>i</LOCAL_VARIABLE> != <PARAMETER>count</PARAMETER>; <LOCAL_VARIABLE>i</LOCAL_VARIABLE>++)
        <FUNCTION_CALL>print</FUNCTION_CALL>("Area: " + <PARAMETER>shape</PARAMETER>.<METHOD_CALL>Area</METHOD_CALL>() + '\n');
    <MACRO>DEBUG_BREAK</MACRO>;
    $
}

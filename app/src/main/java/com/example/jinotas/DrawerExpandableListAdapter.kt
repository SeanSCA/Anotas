package com.example.jinotas

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView

class DrawerExpandableListAdapter(
    private val context: Context,
    private val titleList: List<String>,       // Lista de títulos principales
    private val childMap: Map<String, List<String>> // Mapa de subelementos
) : BaseExpandableListAdapter() {

    override fun getGroupCount() = titleList.size

    override fun getChildrenCount(groupPosition: Int): Int {
        val groupName = titleList[groupPosition]
        return childMap[groupName]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int) = titleList[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        val groupName = titleList[groupPosition]
        return childMap[groupName]?.get(childPosition) ?: ""
    }

    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int) = childPosition.toLong()

    override fun hasStableIds() = false

    override fun isChildSelectable(groupPosition: Int, childPosition: Int) = true

    override fun getGroupView(
        groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?
    ): View {
        val title = getGroup(groupPosition) as String
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)

        val groupTextView = view.findViewById<TextView>(android.R.id.text1)
        groupTextView.text = title
        groupTextView.setTextAppearance(R.style.AldrichTextViewStyle) // Aplica el estilo aquí
        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val item = getChild(groupPosition, childPosition) as String
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false)

        val childTextView = view.findViewById<TextView>(android.R.id.text1)
        childTextView.text = item
        childTextView.setTextAppearance(R.style.AldrichTextViewStyle) // Aplica el estilo aquí
        return view
    }
}

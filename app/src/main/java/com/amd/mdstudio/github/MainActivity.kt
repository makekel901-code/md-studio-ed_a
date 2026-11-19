package com.amd.mdstudio.github

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.amd.mdstudio.legacy.Diagnostics
import com.amd.mdstudio.legacy.PocketAbout
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var projectContainer: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var searchEditText: EditText
    private lateinit var newProjectTitleInput: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var autoSaveCheckBox: CheckBox
    private var isSortAscending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Diagnostics.event("MainActivity created")
        prefs = getSharedPreferences("md_studio_projects", Context.MODE_PRIVATE)
        setupDefaultDataIfNeeded()

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#101D35"))
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // 1. Header Title
        val titleTextView = TextView(this).apply {
            text = "MD Studio — بيئة تطوير التطبيقات"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(12))
        }
        rootLayout.addView(titleTextView)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
        }
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 2. Action Bar / Toolbar Actions
        val topButtonsBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dpToPx(12))
        }

        val aboutBtn = Button(this).apply {
            text = "حول الترخيص"
            setBackgroundColor(Color.parseColor("#1F2E4D"))
            setTextColor(Color.WHITE)
            setOnClickListener { PocketAbout.show(this@MainActivity) }
        }
        topButtonsBar.addView(aboutBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val buildLogBtn = Button(this).apply {
            text = "سجل التشخيصات"
            setBackgroundColor(Color.parseColor("#1F2E4D"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                Diagnostics.event("Manual diagnostic log requested")
                Toast.makeText(this@MainActivity, "تم تسجيل حالة التشخيص بنجاح", Toast.LENGTH_SHORT).show()
            }
        }
        val marginParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dpToPx(8), 0, 0, 0)
        }
        topButtonsBar.addView(buildLogBtn, marginParams)
        contentLayout.addView(topButtonsBar)

        // 3. Section: Create New Project
        val createSectionTitle = TextView(this).apply {
            text = "إنشاء مشروع جديد"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.parseColor("#55D6A2"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(8), 0, dpToPx(6))
        }
        contentLayout.addView(createSectionTitle)

        newProjectTitleInput = EditText(this).apply {
            hint = "اسم المشروع (مثال: MyFirstApp)"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A2942"))
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
        }
        contentLayout.addView(newProjectTitleInput)

        val optionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("تطبيق أندرويد (Kotlin)", "تطبيق أدوات (Java)", "مكتبة كود (Module)"))
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner = Spinner(this).apply {
            adapter = spinnerAdapter
            setBackgroundColor(Color.parseColor("#1A2942"))
        }
        optionsLayout.addView(typeSpinner, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f))

        autoSaveCheckBox = CheckBox(this).apply {
            text = "حفظ تلقائي"
            setTextColor(Color.WHITE)
            isChecked = true
        }
        optionsLayout.addView(autoSaveCheckBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        contentLayout.addView(optionsLayout)

        val createProjectBtn = Button(this).apply {
            text = "+ إضافة المشروع للخطة"
            setBackgroundColor(Color.parseColor("#55D6A2"))
            setTextColor(Color.parseColor("#101D35"))
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                addNewProject()
            }
        }
        contentLayout.addView(createProjectBtn)

        // Divider
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#233554"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(2)).apply {
                setMargins(0, dpToPx(16), 0, dpToPx(16))
            }
        }
        contentLayout.addView(divider)

        // 4. Section: Existing Projects
        val listSectionTitle = TextView(this).apply {
            text = "قائمة المشاريع المتاحة"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.parseColor("#8AB4FF"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(6))
        }
        contentLayout.addView(listSectionTitle)

        // Search & Filter controls
        val searchLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dpToPx(10))
        }

        searchEditText = EditText(this).apply {
            hint = "بحث في المشاريع..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A2942"))
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderList() }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        searchLayout.addView(searchEditText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f))

        val sortBtn = Button(this).apply {
            text = "ترتيب"
            setBackgroundColor(Color.parseColor("#1F2E4D"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                isSortAscending = !isSortAscending
                renderList()
            }
        }
        searchLayout.addView(sortBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dpToPx(6), 0, 0, 0)
        })
        contentLayout.addView(searchLayout)

        // Empty state indicator
        emptyStateText = TextView(this).apply {
            text = "لا يوجد مشاريع مطابقة. أضف مشروعًا جديدًا للبدء."
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(24), 0, dpToPx(24))
            visibility = View.GONE
        }
        contentLayout.addView(emptyStateText)

        // Projects items container
        projectContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentLayout.addView(projectContainer)

        // Clear All Button
        val clearAllBtn = Button(this).apply {
            text = "مسح كافة المشاريع"
            setBackgroundColor(Color.parseColor("#4A1525"))
            setTextColor(Color.parseColor("#FF8A8A"))
            setOnClickListener { confirmClearAll() }
        }
        val clearParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dpToPx(20), 0, dpToPx(12))
        }
        contentLayout.addView(clearAllBtn, clearParams)

        scrollView.addView(contentLayout)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)

        renderList()
    }

    private fun setupDefaultDataIfNeeded() {
        val raw = prefs.getString("items", null)
        if (raw == null) {
            val initial = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1)
                    put("title", "MD Studio Engine")
                    put("type", "تطبيق أندرويد (Kotlin)")
                    put("date", "2025-02-22")
                })
                put(JSONObject().apply {
                    put("id", 2)
                    put("title", "Legacy Reference Tools")
                    put("type", "تطبيق أدوات (Java)")
                    put("date", "2025-02-21")
                })
            }
            prefs.edit().putString("items", initial.toString()).apply()
        }
    }

    private fun getProjects(): MutableList<JSONObject> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val array = JSONArray(raw)
        val list = mutableListOf<JSONObject>()
        for (i in 0 until array.length()) {
            list.add(array.getJSONObject(i))
        }
        return list
    }

    private fun saveProjects(list: List<JSONObject>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString("items", array.toString()).apply()
    }

    private fun addNewProject() {
        val title = newProjectTitleInput.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال اسم المشروع أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        val type = typeSpinner.selectedItem.toString()
        val list = getProjects()
        val newId = (list.maxOfOrNull { it.optInt("id", 0) } ?: 0) + 1
        val newItem = JSONObject().apply {
            put("id", newId)
            put("title", title)
            put("type", type)
            put("date", "اليوم")
        }
        list.add(0, newItem)
        saveProjects(list)
        newProjectTitleInput.setText("")
        Toast.makeText(this, "تمت إضافة المشروع بنجاح", Toast.LENGTH_SHORT).show()
        Diagnostics.event("Project added: $title")
        renderList()
    }

    private fun renderList() {
        projectContainer.removeAllViews()
        val query = searchEditText.text.toString().lowercase()
        var list = getProjects().filter {
            it.optString("title").lowercase().contains(query) ||
            it.optString("type").lowercase().contains(query)
        }

        if (isSortAscending) {
            list = list.sortedBy { it.optString("title") }
        } else {
            list = list.sortedByDescending { it.optInt("id") }
        }

        if (list.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            return
        } else {
            emptyStateText.visibility = View.GONE
        }

        for (item in list) {
            val id = item.optInt("id")
            val title = item.optString("title")
            val type = item.optString("type")
            val date = item.optString("date")

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#1A2942"))
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(8))
                }
                layoutParams = params
            }

            val titleTv = TextView(this).apply {
                text = title
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
            }
            card.addView(titleTv)

            val descTv = TextView(this).apply {
                text = "النوع: $type | التاريخ: $date"
                setTextColor(Color.parseColor("#9CA3AF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dpToPx(4), 0, dpToPx(8))
            }
            card.addView(descTv)

            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val editBtn = Button(this).apply {
                text = "تعديل"
                setBackgroundColor(Color.parseColor("#233554"))
                setTextColor(Color.parseColor("#8AB4FF"))
                setOnClickListener { editProject(id, title) }
            }
            btnRow.addView(editBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val deleteBtn = Button(this).apply {
                text = "حذف"
                setBackgroundColor(Color.parseColor("#3E1B24"))
                setTextColor(Color.parseColor("#FF8A8A"))
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dpToPx(8), 0, 0, 0)
                }
                layoutParams = p
                setOnClickListener { confirmDelete(id) }
            }
            btnRow.addView(deleteBtn)

            card.addView(btnRow)
            projectContainer.addView(card)
        }
    }

    private fun editProject(id: Int, oldTitle: String) {
        val input = EditText(this).apply {
            setText(oldTitle)
            setTextColor(Color.BLACK)
        }
        AlertDialog.Builder(this)
            .setTitle("تعديل اسم المشروع")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    val list = getProjects()
                    list.find { it.optInt("id") == id }?.put("title", newTitle)
                    saveProjects(list)
                    Toast.makeText(this, "تم التعديل", Toast.LENGTH_SHORT).show()
                    renderList()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDelete(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل أنت متأكد من حذف هذا المشروع؟")
            .setPositiveButton("حذف") { _, _ ->
                val list = getProjects().filter { it.optInt("id") != id }
                saveProjects(list)
                Toast.makeText(this, "تم الحذف بنجاح", Toast.LENGTH_SHORT).show()
                renderList()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle("مسح كافة المشاريع")
            .setMessage("سيتم إزالة جميع المشاريع المحفوظة. هل تريد الاستمرار؟")
            .setPositiveButton("مسح الكل") { _, _ ->
                saveProjects(emptyList())
                Toast.makeText(this, "تم مسح جميع المشاريع", Toast.LENGTH_SHORT).show()
                renderList()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}

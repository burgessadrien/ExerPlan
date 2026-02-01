package com.burgessadrien.exerplan.utils

import com.burgessadrien.exerplan.model.DayType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class MooseProgrammingImporterTest {

    @Test
    fun `test importFromStream with multi-week CSV structure`() {
        // Leading commas added to match the real CSV structure where data is in column B (index 1)
        val csvContent = """
            ,GOALS FOR BLOCK ONE
            ,1. Maximize strength
            ,2. Drive hypertrophy
            
            ,WEEK 1,,,,,,,WEEK 2,,,,,,WEEK 3
            ,Day 1 - Squat,Sets,Reps,Load,RPE,Notes,,Day 1 - Squat,Sets,Reps,Load,Notes,,Day 1 - Squat,Sets,Reps,Load,Notes
            
            ,SSB squat,5,5,,8,,,SSB squat,5,5,0,,,SSB squat,5,5,0
            
            ,Day 2 - Press,Sets,Reps,Load,RPE,Notes,,Day 2 - Press,Sets,Reps,Load,Notes,,Day 2 - Press,Sets,Reps,Load,Notes
            
            ,Bench press,5,5,,8,,,Bench press,5,5,0,,,Bench press,5,5,0
            
            ,Day 3 - Hinge,Sets,Reps,Load,RPE,Notes,,Day 3 - Hinge,Sets,Reps,Load,Notes,,Day 3 - Hinge,Sets,Reps,Load,Notes
            
            ,RDL,5,5,,8,,,RDL,5,5,0,,,RDL,5,5,0
            
            ,Day 4 - Pull,Sets,Reps,Load,RPE,Notes,,Day 4 - Pull,Sets,Reps,Load,Notes,,Day 4 - Pull,Sets,Reps,Load,Notes
            
            ,Pull ups,5,3,,8,,,Pull ups,5,3,2.5,,,Pull ups,5,3,5
        """.trimIndent()

        val inputStream = ByteArrayInputStream(csvContent.toByteArray())
        val (blockName, notes, planData) = MooseProgrammingImporter.importFromStream(inputStream, "Test Block")

        assertEquals("Test Block", blockName)
        assertEquals("Should find 2 goal notes", 2, notes.size)
        
        // Each week has 4 working days.
        // Rule: Rest day after every 2 working days.
        // Sequence per week: Working, Working, Rest, Working, Working, Rest. (Total 6 days per week)
        // 3 weeks * 6 days = 18 days total.
        assertEquals(18, planData.size)

        val days = planData.keys.toList()
        
        // Verify Week 1 sequence (Offset 1)
        assertEquals("W1 Day 1 - Squat", days[0].name)
        assertEquals(DayType.WORKING, days[0].dayType)
        assertEquals(1, days[0].day)
        
        assertEquals("W1 Day 2 - Press", days[1].name)
        assertEquals(DayType.WORKING, days[1].dayType)
        assertEquals(2, days[1].day)
        
        // Unique names: "W1 Rest Day 1" and "W1 Rest Day 2"
        assertEquals("W1 Rest Day 1", days[2].name)
        assertEquals(DayType.REST, days[2].dayType)
        assertEquals(3, days[2].day)
        
        assertEquals("W1 Day 3 - Hinge", days[3].name)
        assertEquals(4, days[3].day)
        
        assertEquals("W1 Day 4 - Pull", days[4].name)
        assertEquals(5, days[4].day)
        
        assertEquals("W1 Rest Day 2", days[5].name)
        assertEquals(6, days[5].day)

        // Verify Week 2 sequence (captured Week 2)
        assertEquals("W2 Day 1 - Squat", days[6].name)
        assertEquals(7, days[6].day)
        
        // Verify Week 3 sequence (captured Week 3)
        assertEquals("W3 Day 1 - Squat", days[12].name)
        assertEquals(13, days[12].day)

        // Verify exercise data for Week 1 (with RPE)
        val w1d1Workouts = planData[days[0]]!!
        assertEquals(1, w1d1Workouts.size)
        assertEquals("SSB squat", w1d1Workouts[0].exerciseName)
        assertEquals("8", w1d1Workouts[0].rpe)

        // Verify exercise data for Week 2 (No RPE column, verify shift)
        val w2d1Workouts = planData[days[6]]!!
        assertEquals(1, w2d1Workouts.size)
        assertEquals("SSB squat", w2d1Workouts[0].exerciseName)
        assertEquals("", w2d1Workouts[0].rpe)
    }
}

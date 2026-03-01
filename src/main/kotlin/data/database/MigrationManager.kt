package com.amos_tech_code.data.database

import org.jetbrains.exposed.sql.transactions.transaction

object MigrationManager {

    /**
     * Run SQL migrations
     */
    fun migrate() {

        /*transaction {
            try {
                // Add fcm_token column to lecturers table if it doesn't exist
                exec(
                    """
                    ALTER TABLE lecturers
                    ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(255);
                """
                )

                // Create index for faster lookups (optional but recommended)
                exec(
                    """
                    CREATE INDEX IF NOT EXISTS idx_lecturers_fcm_token 
                    ON lecturers(fcm_token) 
                    WHERE fcm_token IS NOT NULL;
                """
                )

                println("✅ Added fcm_token column to lecturers table")

            } catch (e: Exception) {
                println("❌ Migration failed: ${e.message}")
                throw e
            }
        }

         */
        /*
        transaction {

            exec("""
                ALTER TABLE student_devices
                ADD COLUMN IF NOT EXISTS status VARCHAR(20);
            """)

            exec("""
                UPDATE student_devices
                SET status = 'ACTIVE'
                WHERE status IS NULL;
            """)

            exec("""
                ALTER TABLE student_devices
                ALTER COLUMN status SET DEFAULT 'ACTIVE';
            """)

            exec("""
                ALTER TABLE student_devices
                ALTER COLUMN status SET NOT NULL;
            """)

            exec("""
                CREATE INDEX IF NOT EXISTS idx_device_student_status
                ON student_devices(student_id, status);
            """)

            exec("""
                CREATE INDEX IF NOT EXISTS idx_device_device_id
                ON student_devices(device_id);
            """)

            println("✅ Database migrations completed")
        }

         */
    }
}